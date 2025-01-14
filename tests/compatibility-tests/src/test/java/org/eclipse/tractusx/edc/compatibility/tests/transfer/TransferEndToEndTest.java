/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests.transfer;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.BaseParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.DataspaceIssuer;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.EdcDockerRuntimes;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.IdentityHubParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.LocalParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.RemoteParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.Runtimes;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipantContext;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;


@EndToEndTest
public class TransferEndToEndTest {

    protected static final IdentityHubParticipant IDENTITY_HUB_PARTICIPANT = IdentityHubParticipant.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();
    
    protected static final DataspaceIssuer ISSUER = DataspaceIssuer.Builder.newInstance().id("issuer").name("issuer")
            .did(IDENTITY_HUB_PARTICIPANT.didFor("issuer"))
            .build();

    protected static final RemoteParticipant REMOTE_PARTICIPANT = RemoteParticipant.Builder.newInstance()
            .name("remote")
            .id("remote")
            .sts(IDENTITY_HUB_PARTICIPANT.getSts())
            .did(IDENTITY_HUB_PARTICIPANT.didFor("remote"))
            .trustedIssuer(ISSUER.getDid())
            .build();

    protected static final LocalParticipant LOCAL_PARTICIPANT = LocalParticipant.Builder.newInstance()
            .name("local")
            .id("local")
            .sts(IDENTITY_HUB_PARTICIPANT.getSts())
            .did(IDENTITY_HUB_PARTICIPANT.didFor("local"))
            .trustedIssuer(ISSUER.getDid())
            .build();

    private static final GenericContainer<?> REMOTE_DATA_PLANE = EdcDockerRuntimes.DATA_PLANE.create("dataplane", REMOTE_PARTICIPANT.dataPlaneEnv(LOCAL_PARTICIPANT));
    private static final GenericContainer<?> REMOTE_CONTROL_PLANE = EdcDockerRuntimes.CONTROL_PLANE.create("controlplane", REMOTE_PARTICIPANT.controlPlaneEnv(LOCAL_PARTICIPANT));

    @Order(1)
    @RegisterExtension
    static final RuntimeExtension LOCAL_CONTROL_PLANE = new RuntimePerClassExtension(
            Runtimes.CONTROL_PLANE.create("local-control-plane", LOCAL_PARTICIPANT.controlPlanePostgresConfiguration()));

    @Order(2)
    @RegisterExtension
    static final RuntimeExtension LOCAL_DATA_PLANE = new RuntimePerClassExtension(
            Runtimes.DATA_PLANE.create("local-data-plane", LOCAL_PARTICIPANT.dataPlanePostgresConfiguration()));

    @Order(1)
    @RegisterExtension
    static final RuntimeExtension LOCAL_IDENTITY_HUB = new RuntimePerClassExtension(
            Runtimes.IDENTITY_HUB.create("local-identity-hub", IDENTITY_HUB_PARTICIPANT.getConfiguration()));

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.4")
            .withUsername("postgres")
            .withPassword("password")
            .withCreateContainerCmdModifier(cmd -> cmd.withName("postgres"));

    @Order(0)
    @RegisterExtension
    static final BeforeAllCallback BOOSTRAP = context -> {
        var dids = Map.of(LOCAL_PARTICIPANT.getId(), LOCAL_PARTICIPANT.getDid(), REMOTE_PARTICIPANT.getId(), REMOTE_PARTICIPANT.getDid());
        LOCAL_CONTROL_PLANE.registerServiceMock(BdrsClient.class, dids::get);
        LOCAL_CONTROL_PLANE.registerServiceMock(AudienceResolver.class, message -> Result.success(dids.get(message.getCounterPartyId())));

        PG.setPortBindings(List.of("5432:5432"));
        PG.start();
        createDatabase(LOCAL_PARTICIPANT.getName());
        createDatabase(REMOTE_PARTICIPANT.getName());
    };

    private static ClientAndServer providerDataSource;

    @BeforeAll
    static void beforeAll() {
        REMOTE_CONTROL_PLANE.start();
        REMOTE_DATA_PLANE.start();
        providerDataSource = startClientAndServer(getFreePort());
        configureParticipant(LOCAL_PARTICIPANT, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        configureParticipant(REMOTE_PARTICIPANT, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        configureParticipantContext(ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);

        var vault = LOCAL_DATA_PLANE.getService(Vault.class);
        vault.storeSecret("private-key", LOCAL_PARTICIPANT.getPrivateKeyAsString());
        vault.storeSecret("public-key", LOCAL_PARTICIPANT.getPublicKeyAsString());
        vault.storeSecret("local-secret", "clientSecret");

        var cpVault = LOCAL_CONTROL_PLANE.getService(Vault.class);
        cpVault.storeSecret("local-secret", "clientSecret");
    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantsArgProvider.class)
    void httpPullTransfer(BaseParticipant consumer, BaseParticipant provider, String protocol) {
        consumer.setProtocol(protocol);
        provider.setProtocol(protocol);
        provider.waitForDataPlane();
        providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response().withBody("data"));
        var assetId = UUID.randomUUID().toString();
        var sourceDataAddress = httpSourceDataAddress();
        createResourcesOnProvider(provider, assetId, PolicyFixtures.contractExpiresIn("5s"), sourceDataAddress);

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PULL")
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);

        var edr = await().atMost(consumer.getTimeout())
                .until(() -> consumer.getEdr(transferProcessId), Objects::nonNull);

        // Do the transfer
        var msg = UUID.randomUUID().toString();
        await().atMost(consumer.getTimeout())
                .untilAsserted(() -> consumer.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data")));

        // checks that the EDR is gone once the contract expires
        await().atMost(consumer.getTimeout())
                .untilAsserted(() -> assertThatThrownBy(() -> consumer.getEdr(transferProcessId)));

        // checks that transfer fails
        await().atMost(consumer.getTimeout())
                .untilAsserted(() -> assertThatThrownBy(() -> consumer.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data"))));

        providerDataSource.verify(HttpRequest.request("/source").withMethod("GET"));

    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantsArgProvider.class)
    void suspendAndResume_httpPull_dataTransfer(BaseParticipant consumer, BaseParticipant provider, String protocol) {
        consumer.setProtocol(protocol);
        provider.setProtocol(protocol);
        provider.waitForDataPlane();
        providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response().withBody("data"));
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(provider, assetId, PolicyFixtures.noConstraintPolicy(), httpSourceDataAddress());

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PULL")
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);

        var edr = await().atMost(consumer.getTimeout()).until(() -> consumer.getEdr(transferProcessId), Objects::nonNull);

        var msg = UUID.randomUUID().toString();
        await().atMost(consumer.getTimeout()).untilAsserted(() -> consumer.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data")));

        consumer.suspendTransfer(transferProcessId, "supension");

        consumer.awaitTransferToBeInState(transferProcessId, SUSPENDED);

        // checks that the EDR is gone once the transfer has been suspended
        await().atMost(consumer.getTimeout()).untilAsserted(() -> assertThatThrownBy(() -> consumer.getEdr(transferProcessId)));
        // checks that transfer fails
        await().atMost(consumer.getTimeout()).untilAsserted(() -> assertThatThrownBy(() -> consumer.pullData(edr, Map.of("message", msg), body -> assertThat(body).isEqualTo("data"))));

        consumer.resumeTransfer(transferProcessId);

        // check that transfer is available again
        consumer.awaitTransferToBeInState(transferProcessId, STARTED);
        var secondEdr = await().atMost(consumer.getTimeout()).until(() -> consumer.getEdr(transferProcessId), Objects::nonNull);
        var secondMessage = UUID.randomUUID().toString();
        await().atMost(consumer.getTimeout()).untilAsserted(() -> consumer.pullData(secondEdr, Map.of("message", secondMessage), body -> assertThat(body).isEqualTo("data")));

        providerDataSource.verify(HttpRequest.request("/source").withMethod("GET"));
    }

    protected void createResourcesOnProvider(BaseParticipant provider, String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        provider.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var contractPolicyId = provider.createPolicyDefinition(contractPolicy);
        var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());

        provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, contractPolicyId);
    }

    private @NotNull Map<String, Object> httpSourceDataAddress() {
        return Map.of(
                EDC_NAMESPACE + "name", "transfer-test",
                EDC_NAMESPACE + "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "proxyQueryParams", "true"
        );
    }

    private static class ParticipantsArgProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(REMOTE_PARTICIPANT, LOCAL_PARTICIPANT, "dataspace-protocol-http"),
                    Arguments.of(LOCAL_PARTICIPANT, REMOTE_PARTICIPANT, "dataspace-protocol-http")
            );
        }
    }

}
