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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class RemoteParticipant extends BaseParticipant {

    private static final String API_KEY = "password";

    private final List<String> datasources = List.of("asset", "contractdefinition",
            "contractnegotiation", "policy", "transferprocess", "bpn",
            "policy-monitor", "edr", "dataplane", "accesstokendata", "dataplaneinstance");

    public Map<String, String> controlPlaneEnv(BaseParticipant participant) {
        return new HashMap<>() {
            {
                put("EDC_PARTICIPANT_ID", id);
                put("EDC_API_AUTH_KEY", API_KEY);
                put("WEB_HTTP_PORT", String.valueOf(getFreePort()));
                put("WEB_HTTP_PATH", "/api");
                put("WEB_HTTP_PROTOCOL_PORT", String.valueOf(controlPlaneProtocol.get().getPort()));
                put("WEB_HTTP_PROTOCOL_PATH", controlPlaneProtocol.get().getPath());
                put("WEB_HTTP_MANAGEMENT_PORT", String.valueOf(controlPlaneManagement.get().getPort()));
                put("WEB_HTTP_MANAGEMENT_PATH", controlPlaneManagement.get().getPath());
                put("WEB_HTTP_VERSION_PORT", String.valueOf(controlPlaneVersion.get().getPort()));
                put("WEB_HTTP_VERSION_PATH", controlPlaneVersion.get().getPath());
                put("WEB_HTTP_CONTROL_PORT", String.valueOf(controlPlaneControl.get().getPort()));
                put("WEB_HTTP_CONTROL_PATH", controlPlaneControl.get().getPath());
                put("WEB_HTTP_CATALOG_PORT", String.valueOf(getFreePort()));
                put("WEB_HTTP_CATALOG_PATH", "/catalog");
                put("EDC_DSP_CALLBACK_ADDRESS", controlPlaneProtocol.get().toString());
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
                put("TESTING_EDC_BDRS_1_KEY", participant.getId());
                put("TESTING_EDC_BDRS_1_VALUE", participant.getDid());
                put("EDC_IAM_TRUSTED-ISSUER_ISSUER_ID", trustedIssuer);
                putAll(datasourceConfig());
            }
        };
    }

    public Map<String, String> dataPlaneEnv(BaseParticipant participant) {

        return new HashMap<>() {
            {
                put("EDC_PARTICIPANT_ID", id);
                put("EDC_COMPONENT_ID", id);
                put("EDC_API_AUTH_KEY", API_KEY);
                put("WEB_HTTP_PORT", String.valueOf(getFreePort()));
                put("WEB_HTTP_PATH", "/api");
                put("WEB_HTTP_VERSION_PORT", String.valueOf(dataPlaneVersion.get().getPort()));
                put("WEB_HTTP_VERSION_PATH", dataPlaneVersion.get().getPath());
                put("WEB_HTTP_CONTROL_PORT", String.valueOf(dataPlaneControl.get().getPort()));
                put("WEB_HTTP_CONTROL_PATH", dataPlaneControl.get().getPath());
                put("WEB_HTTP_PUBLIC_PORT", String.valueOf(dataPlanePublic.get().getPort()));
                put("WEB_HTTP_PUBLIC_PATH", dataPlanePublic.get().getPath());
                put("TX_EDC_DPF_CONSUMER_PROXY_PORT", String.valueOf(consumerPublic.get().getPort()));
                put("EDC_DATASOURCE_DEFAULT_URL", "jdbc:postgresql://localhost:5432/%s".formatted(getId()));
                put("EDC_DATASOURCE_DEFAULT_USER", "postgres");
                put("EDC_DATASOURCE_DEFAULT_PASSWORD", "password");
                put("EDC_TRANSFER_PROXY_TOKEN_SIGNER_PRIVATEKEY_ALIAS", "private-key");
                put("EDC_TRANSFER_PROXY_TOKEN_VERIFIER_PUBLICKEY_ALIAS", "public-key");
                put("EDC_DPF_SELECTOR_URL", controlPlaneControl.get() + "/v1/dataplanes");
                put("EDC_IAM_STS_OAUTH_TOKEN_URL", sts.toString() + "/token");
                put("EDC_IAM_STS_OAUTH_CLIENT_ID", getDid());
                put("EDC_IAM_STS_OAUTH_CLIENT_SECRET_ALIAS", id + "-secret");
                put("TESTING_EDC_VAULTS_1_KEY", id + "-secret");
                put("TESTING_EDC_VAULTS_1_VALUE", "clientSecret");
                put("TESTING_EDC_VAULTS_2_KEY", "private-key");
                put("TESTING_EDC_VAULTS_2_VALUE", getPrivateKeyAsString());
                put("TESTING_EDC_VAULTS_3_KEY", "public-key");
                put("TESTING_EDC_VAULTS_3_VALUE", getPublicKeyAsString());
                put("TESTING_EDC_BDRS_1_KEY", participant.getId());
                put("TESTING_EDC_BDRS_1_VALUE", participant.getDid());
                put("EDC_IAM_ISSUER_ID", getDid());
                put("EDC_IAM_TRUSTED-ISSUER_ISSUER_ID", trustedIssuer);

                putAll(datasourceConfig());
            }
        };
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
            this.participant.enrichManagementRequest = request -> request.header("x-api-key", API_KEY);
            return super.build();
        }
    }
}
