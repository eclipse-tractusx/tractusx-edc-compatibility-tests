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

package org.eclipse.tractusx.edc.compatibility.tests.fixtures;

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.defaultDatasourceConfiguration;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class LocalParticipant extends BaseParticipant {

    private static final String API_KEY = "password";

    private final int httpProvisionerPort = getFreePort();

    public Config controlPlanePostgresConfig() {
        return controlPlaneConfig().merge(defaultDatasourceConfig());
    }

    public Config dataPlanePostgresConfig() {
        return dataPlaneConfig().merge(defaultDatasourceConfig());
    }

    private Config controlPlaneConfig() {
        Map<String, String> settings = new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("edc.api.auth.key", API_KEY);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.protocol.port", String.valueOf(controlPlaneProtocol.get().getPort()));
                put("web.http.protocol.path", controlPlaneProtocol.get().getPath());
                put("web.http.management.port", String.valueOf(controlPlaneManagement.get().getPort()));
                put("web.http.management.path", controlPlaneManagement.get().getPath());
                put("web.http.version.port", String.valueOf(controlPlaneVersion.get().getPort()));
                put("web.http.version.path", controlPlaneVersion.get().getPath());
                put("web.http.control.port", String.valueOf(controlPlaneControl.get().getPort()));
                put("web.http.control.path", controlPlaneControl.get().getPath());
                put("edc.dsp.callback.address", controlPlaneProtocol.get().toString());
                put("edc.transfer.proxy.endpoint", dataPlanePublic.get().toString());
                put("edc.transfer.send.retry.limit", "1");
                put("edc.transfer.send.retry.base-delay.ms", "100");
                put("edc.negotiation.consumer.send.retry.limit", "1");
                put("edc.negotiation.provider.send.retry.limit", "1");
                put("edc.negotiation.consumer.send.retry.base-delay.ms", "100");
                put("edc.negotiation.provider.send.retry.base-delay.ms", "100");

                put("edc.negotiation.consumer.state-machine.iteration-wait-millis", "50");
                put("edc.negotiation.provider.state-machine.iteration-wait-millis", "50");
                put("edc.transfer.state-machine.iteration-wait-millis", "50");

                put("provisioner.http.entries.default.provisioner.type", "provider");
                put("provisioner.http.entries.default.endpoint", "http://localhost:%d/provision".formatted(httpProvisionerPort));
                put("provisioner.http.entries.default.data.address.type", "HttpProvision");
                put("edc.iam.sts.oauth.token.url", sts.toString() + "/token");
                put("edc.iam.sts.oauth.client.id", getDid());
                put("edc.iam.sts.oauth.client.secret.alias", id + "-secret");
                put("edc.iam.issuer.id", getDid());
                put("edc.iam.did.web.use.https", "false");
                put("edc.iam.trusted-issuer.issuer.id", trustedIssuer);
            }
        };

        return ConfigFactory.fromMap(settings);
    }

    private Config dataPlaneConfig() {
        Map<String, String> settings = new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.public.port", String.valueOf(dataPlanePublic.get().getPort()));
                put("web.http.public.path", dataPlanePublic.get().getPath());
                put("web.http.control.port", String.valueOf(dataPlaneControl.get().getPort()));
                put("web.http.control.path", dataPlaneControl.get().getPath());
                put("edc.dataplane.api.public.baseurl", dataPlanePublic.get() + "/v2/");
                put("edc.dataplane.token.validation.endpoint", controlPlaneControl.get() + "/token");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.dataplane.http.sink.partition.size", "1");
                put("edc.dataplane.state-machine.iteration-wait-millis", "50");
                put("edc.dpf.selector.url", controlPlaneControl.get() + "/v1/dataplanes");
                put("edc.component.id", "dataplane");
                put("edc.iam.sts.oauth.token.url", sts.toString() + "/token");
                put("edc.iam.sts.oauth.client.id", getDid());
                put("edc.iam.sts.oauth.client.secret.alias", id + "-secret");
                put("edc.iam.issuer.id", getDid());
                put("edc.iam.trusted-issuer.issuer.id", trustedIssuer);
            }
        };

        return ConfigFactory.fromMap(settings);
    }

    private Config defaultDatasourceConfig() {
        return ConfigFactory.fromMap(defaultDatasourceConfiguration(getName()));
    }

    public static class Builder extends BaseParticipant.Builder<LocalParticipant, Builder> {

        protected Builder() {
            super(new LocalParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public LocalParticipant build() {
            this.participant.enrichManagementRequest = request -> request.header("x-api-key", API_KEY);
            return super.build();
        }
    }
}
