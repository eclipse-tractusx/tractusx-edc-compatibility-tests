/*******************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.edc.compatibility.tests.fixtures;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.defaultDatasourceConfiguration;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class LocalParticipant extends BaseParticipant {

    private static final String API_KEY = "password";

    private final int httpProvisionerPort = getFreePort();

    public Map<String, String> controlPlaneConfiguration() {
        return new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("edc.api.auth.key", API_KEY);
                put("web.http.port", String.valueOf(controlPlaneDefault.getPort()));
                put("web.http.path", "/api");
                put("web.http.protocol.port", String.valueOf(protocolEndpoint.getUrl().getPort()));
                put("web.http.protocol.path", protocolEndpoint.getUrl().getPath());
                put("web.http.management.port", String.valueOf(managementEndpoint.getUrl().getPort()));
                put("web.http.management.path", managementEndpoint.getUrl().getPath());
                put("web.http.version.port", String.valueOf(controlPlaneVersion.getPort()));
                put("web.http.version.path", controlPlaneVersion.getPath());
                put("web.http.control.port", String.valueOf(controlPlaneControl.getPort()));
                put("web.http.control.path", controlPlaneControl.getPath());
                put("edc.dsp.callback.address", protocolEndpoint.getUrl().toString());
                put("edc.transfer.proxy.endpoint", dataPlanePublic.toString());
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
    }

    public Map<String, String> controlPlanePostgresConfiguration() {
        var baseConfiguration = controlPlaneConfiguration();
        baseConfiguration.putAll(defaultDatasourceConfiguration(getName()));
        return baseConfiguration;
    }

    public Map<String, String> dataPlaneConfiguration() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(dataPlaneDefault.getPort()));
                put("web.http.path", "/api");
                put("web.http.public.port", String.valueOf(dataPlanePublic.getPort()));
                put("web.http.public.path", "/public");
                put("web.http.control.port", String.valueOf(dataPlaneControl.getPort()));
                put("web.http.control.path", dataPlaneControl.getPath());
                put("edc.dataplane.api.public.baseurl", dataPlanePublic + "/v2/");
                put("edc.dataplane.token.validation.endpoint", controlPlaneControl + "/token");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.dataplane.http.sink.partition.size", "1");
                put("edc.dataplane.state-machine.iteration-wait-millis", "50");
                put("edc.dpf.selector.url", controlPlaneControl + "/v1/dataplanes");
                put("edc.component.id", "dataplane");
                put("edc.iam.sts.oauth.token.url", sts.toString() + "/token");
                put("edc.iam.sts.oauth.client.id", getDid());
                put("edc.iam.sts.oauth.client.secret.alias", id + "-secret");
                put("edc.iam.issuer.id", getDid());
                put("edc.iam.trusted-issuer.issuer.id", trustedIssuer);
            }
        };
    }

    public Map<String, String> dataPlanePostgresConfiguration() {
        var baseConfiguration = dataPlaneConfiguration();
        baseConfiguration.putAll(defaultDatasourceConfiguration(getName()));
        return baseConfiguration;
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
            var headers = Map.of("x-api-key", API_KEY);
            super.managementEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/management"), headers));
            super.protocolEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/protocol")));
            super.build();
            return participant;
        }
    }
}
