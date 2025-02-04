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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class RemoteParticipant extends BaseParticipant {

    private static final String API_KEY = "password";

    private final List<String> datasources = List.of("asset", "contractdefinition",
            "contractnegotiation", "policy", "transferprocess", "bpn",
            "policy-monitor", "edr", "dataplane", "accesstokendata", "dataplaneinstance");

    public Map<String, String> controlPlaneEnv(List<BaseParticipant> participants) {

        Map<String, String> env = new HashMap<>() {
            {
                put("EDC_PARTICIPANT_ID", id);
                put("EDC_API_AUTH_KEY", API_KEY);
                put("WEB_HTTP_PORT", String.valueOf(controlPlaneDefault.getPort()));
                put("WEB_HTTP_PATH", "/api");
                put("WEB_HTTP_PROTOCOL_PORT", String.valueOf(protocolEndpoint.getUrl().getPort()));
                put("WEB_HTTP_PROTOCOL_PATH", protocolEndpoint.getUrl().getPath());
                put("WEB_HTTP_MANAGEMENT_PORT", String.valueOf(managementEndpoint.getUrl().getPort()));
                put("WEB_HTTP_MANAGEMENT_PATH", managementEndpoint.getUrl().getPath());
                put("WEB_HTTP_VERSION_PORT", String.valueOf(controlPlaneVersion.getPort()));
                put("WEB_HTTP_VERSION_PATH", controlPlaneVersion.getPath());
                put("WEB_HTTP_CONTROL_PORT", String.valueOf(controlPlaneControl.getPort()));
                put("WEB_HTTP_CONTROL_PATH", controlPlaneControl.getPath());
                put("WEB_HTTP_CATALOG_PORT", String.valueOf(getFreePort()));
                put("WEB_HTTP_CATALOG_PATH", "/catalog");
                put("EDC_DSP_CALLBACK_ADDRESS", protocolEndpoint.getUrl().toString());
                put("EDC_DATASOURCE_DEFAULT_URL", "jdbc:postgresql://localhost:5432/%s".formatted(getId()));
                put("EDC_DATASOURCE_DEFAULT_USER", "postgres");
                put("EDC_DATASOURCE_DEFAULT_PASSWORD", "password");
                put("EDC_IAM_STS_OAUTH_TOKEN_URL", sts.toString() + "/token");
                put("EDC_IAM_STS_OAUTH_CLIENT_ID", getDid());
                put("EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS", id + "-secret");
                put("TESTING_EDC_VAULTS_1_KEY", id + "-secret");
                put("TESTING_EDC_VAULTS_1_VALUE", "clientSecret");
                put("EDC_IAM_ISSUER_ID", getDid());
                put("EDC_IAM_DID_WEB_USE_HTTPS", "false");
                put("EDC_IAM_TRUSTED-ISSUER_ISSUER_ID", trustedIssuer);
                putAll(datasourceConfig());
            }
        };
        addBdrsEnvs(env, participants);
        return env;
    }

    private void addBdrsEnvs(Map<String, String> env, List<BaseParticipant> participants) {
        for (int i = 0; i < participants.size(); i++) {
            env.put("TESTING_EDC_BDRS_" + (i + 1) + "_KEY", participants.get(i).getId());
            env.put("TESTING_EDC_BDRS_" + (i + 1) + "_VALUE", participants.get(i).getDid());
        }
    }


    public Map<String, String> dataPlaneEnv(List<BaseParticipant> participant) {
        Map<String, String> envs = new HashMap<>() {
            {
                put("EDC_PARTICIPANT_ID", id);
                put("EDC_COMPONENT_ID", id);
                put("EDC_API_AUTH_KEY", API_KEY);
                put("WEB_HTTP_PORT", String.valueOf(dataPlaneDefault.getPort()));
                put("WEB_HTTP_PATH", "/api");
                put("WEB_HTTP_VERSION_PORT", String.valueOf(dataPlaneVersion.getPort()));
                put("WEB_HTTP_VERSION_PATH", dataPlaneVersion.getPath());
                put("WEB_HTTP_CONTROL_PORT", String.valueOf(dataPlaneControl.getPort()));
                put("WEB_HTTP_CONTROL_PATH", dataPlaneControl.getPath());
                put("WEB_HTTP_PUBLIC_PORT", String.valueOf(dataPlanePublic.getPort()));
                put("WEB_HTTP_PUBLIC_PATH", dataPlanePublic.getPath());
                put("TX_EDC_DPF_CONSUMER_PROXY_PORT", String.valueOf(consumerPublic.getPort()));
                put("EDC_DATASOURCE_DEFAULT_URL", "jdbc:postgresql://localhost:5432/%s".formatted(getId()));
                put("EDC_DATASOURCE_DEFAULT_USER", "postgres");
                put("EDC_DATASOURCE_DEFAULT_PASSWORD", "password");
                put("EDC_TRANSFER_PROXY_TOKEN_SIGNER_PRIVATEKEY_ALIAS", "private-key");
                put("EDC_TRANSFER_PROXY_TOKEN_VERIFIER_PUBLICKEY_ALIAS", "public-key");
                put("EDC_DPF_SELECTOR_URL", controlPlaneControl + "/v1/dataplanes");
                put("EDC_IAM_STS_OAUTH_TOKEN_URL", sts.toString() + "/token");
                put("EDC_IAM_STS_OAUTH_CLIENT_ID", getDid());
                put("EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS", id + "-secret");
                put("TESTING_EDC_VAULTS_1_KEY", id + "-secret");
                put("TESTING_EDC_VAULTS_1_VALUE", "clientSecret");
                put("TESTING_EDC_VAULTS_2_KEY", "private-key");
                put("TESTING_EDC_VAULTS_2_VALUE", getPrivateKeyAsString());
                put("TESTING_EDC_VAULTS_3_KEY", "public-key");
                put("TESTING_EDC_VAULTS_3_VALUE", getPublicKeyAsString());
                put("EDC_IAM_ISSUER_ID", getDid());
                put("EDC_IAM_TRUSTED-ISSUER_ISSUER_ID", trustedIssuer);

                putAll(datasourceConfig());
            }
        };
        addBdrsEnvs(envs, participant);
        return envs;
    }

    private Map<String, String> datasourceConfig() {
        var config = new HashMap<String, String>();
        datasources.forEach(ds -> {
            config.put("EDC_DATASOURCE_" + ds.toUpperCase() + "_URL", "jdbc:postgresql://localhost:5432/" + getName());
            config.put("EDC_DATASOURCE_" + ds.toUpperCase() + "_NAME", ds);
            config.put("EDC_DATASOURCE_" + ds.toUpperCase() + "_USER", "postgres");
            config.put("EDC_DATASOURCE_" + ds.toUpperCase() + "_PASSWORD", "password");
        });
        return config;
    }

    public static class Builder extends BaseParticipant.Builder<RemoteParticipant, Builder> {

        protected Builder() {
            super(new RemoteParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public RemoteParticipant build() {
            var headers = Map.of("x-api-key", API_KEY);
            super.managementEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/management"), headers));
            super.protocolEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/protocol")));
            super.build();
            return participant;
        }
    }
}
