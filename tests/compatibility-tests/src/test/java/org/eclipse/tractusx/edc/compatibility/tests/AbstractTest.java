/*******************************************************************************
 * Copyright (c) 2025 SAP SE
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

package org.eclipse.tractusx.edc.compatibility.tests;

import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
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
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipantContext;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public class AbstractTest {
    private static final List<String> PROTOCOLS_TO_TEST = List.of("dataspace-protocol-http");
    private static final List<EdcDockerRuntimes> RUNTIMES_TO_TEST = List.of(EdcDockerRuntimes.STABLE_CONNECTOR, EdcDockerRuntimes.STABLE_CONNECTOR_0_7_6);

    protected static final IdentityHubParticipant IDENTITY_HUB_PARTICIPANT = IdentityHubParticipant.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();

    protected static final DataspaceIssuer ISSUER = DataspaceIssuer.Builder.newInstance().id("issuer").name("issuer")
            .did(IDENTITY_HUB_PARTICIPANT.didFor("issuer"))
            .build();


    protected static final LocalParticipant LOCAL_PARTICIPANT = LocalParticipant.Builder.newInstance()
            .name("local")
            .id("local")
            .sts(IDENTITY_HUB_PARTICIPANT.getSts())
            .did(IDENTITY_HUB_PARTICIPANT.didFor("local"))
            .trustedIssuer(ISSUER.getDid())
            .build();


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

    private static final Map<EdcDockerRuntimes, RemoteParticipant> REMOTE_PARTICIPANT_MAP = new HashMap<>();

    private static RemoteParticipant createParticipant(EdcDockerRuntimes runtime) {
        String runtimeName = runtime.name().toLowerCase();
        return RemoteParticipant.Builder.newInstance()
                .name(runtimeName)
                .id(runtimeName)
                .sts(IDENTITY_HUB_PARTICIPANT.getSts())
                .did(IDENTITY_HUB_PARTICIPANT.didFor(runtimeName))
                .trustedIssuer(ISSUER.getDid())
                .build();
    }

    @Order(0)
    @RegisterExtension
    static final BeforeAllCallback BOOSTRAP = context -> {
        RUNTIMES_TO_TEST.forEach(runtime -> {
            REMOTE_PARTICIPANT_MAP.put(runtime, createParticipant(runtime));
        });

        PG.setPortBindings(List.of("5432:5432"));
        PG.start();
        createDatabase(LOCAL_PARTICIPANT.getName());
        Map<String, String> dids = new HashMap<>();
        REMOTE_PARTICIPANT_MAP.values().forEach(participant -> {
            dids.put(participant.getId(), participant.getDid());
            createDatabase(participant.getName());
        });
        dids.put(LOCAL_PARTICIPANT.getId(), LOCAL_PARTICIPANT.getDid());
        LOCAL_CONTROL_PLANE.registerServiceMock(BdrsClient.class, dids::get);
        LOCAL_CONTROL_PLANE.registerServiceMock(AudienceResolver.class, message -> Result.success(dids.get(message.getCounterPartyId())));
    };

    protected static ClientAndServer providerDataSource;


    private static void startRemoteRuntimes() {
        REMOTE_PARTICIPANT_MAP.forEach((runtime, participant) -> {
            List<BaseParticipant> participants = new ArrayList<>(REMOTE_PARTICIPANT_MAP.values().stream().filter(p -> !p.equals(participant)).toList());
            participants.add(LOCAL_PARTICIPANT);
            runtime.start(participant.controlPlaneEnv(participants), participant.dataPlaneEnv(participants));
            configureParticipant(participant, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        });
    }

    @BeforeAll
    static void beforeAll() {
        configureParticipant(LOCAL_PARTICIPANT, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        startRemoteRuntimes();
        configureParticipantContext(ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);

        providerDataSource = startClientAndServer(getFreePort());
        providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response().withBody("data"));

        var vault = LOCAL_DATA_PLANE.getService(Vault.class);
        vault.storeSecret("private-key", LOCAL_PARTICIPANT.getPrivateKeyAsString());
        vault.storeSecret("public-key", LOCAL_PARTICIPANT.getPublicKeyAsString());
        vault.storeSecret("local-secret", "clientSecret");

        var cpVault = LOCAL_CONTROL_PLANE.getService(Vault.class);
        cpVault.storeSecret("local-secret", "clientSecret");
    }

    protected void initialise(BaseParticipant consumer, BaseParticipant provider, String protocol) {
        provider.setProtocol(protocol);
        consumer.setProtocol(protocol);
        provider.waitForDataPlane();
        providerDataSource.when(HttpRequest.request()).respond(HttpResponse.response("/source").withBody("data"));
    }

    protected @NotNull Map<String, Object> httpSourceDataAddress() {
        return Map.of(EDC_NAMESPACE + "name", "transfer-test", EDC_NAMESPACE + "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source", EDC_NAMESPACE + "type", "HttpData", EDC_NAMESPACE + "proxyQueryParams", "true");
    }

    protected static class ParticipantsArgProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return createArgumentMatrix().stream();
        }

    }

    private static List<Arguments> createArgumentMatrix() {
        List<Arguments> testArguments = new ArrayList<>();
        List<Participant> participants = new ArrayList<>(REMOTE_PARTICIPANT_MAP.values());
        participants.add(LOCAL_PARTICIPANT);
        for (int i = 0; i < participants.size(); i++) {
            Participant consumer = participants.get(i);
            for (int j = i + 1; j < participants.size(); j++) {
                Participant provider = participants.get(j);
                for (String protocol : PROTOCOLS_TO_TEST) {
                    testArguments.add(Arguments.of(Named.of(consumer.getName(), consumer), Named.of(provider.getName(), provider), protocol));
                    testArguments.add(Arguments.of(Named.of(provider.getName(), provider), Named.of(consumer.getName(), consumer), protocol));
                }
            }
        }
        return testArguments;
    }


}
