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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class IdentityHubParticipant {

    protected final URI defaultApi = URI.create("http://localhost:" + getFreePort() + "/api");


    protected final URI sts = URI.create("http://localhost:" + getFreePort() + "/api/sts");
    protected final URI accountsApi = URI.create("http://localhost:" + getFreePort() + "/api/accounts");
    protected final URI resolutionApi = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    protected final URI identityApi = URI.create("http://localhost:" + getFreePort() + "/api/identity");
    protected final URI didApi = URI.create("http://localhost:" + getFreePort() + "/");
    protected String id;
    protected String name;


    public Map<String, String> getConfiguration() {
        Map<String, String> config = new HashMap<>();
        config.put("web.http.port", String.valueOf(defaultApi.getPort()));
        config.put("web.http.path", defaultApi.getPath());
        config.put("web.http.presentation.port", String.valueOf(resolutionApi.getPort()));
        config.put("web.http.presentation.path", resolutionApi.getPath());
        config.put("web.http.identity.port", String.valueOf(identityApi.getPort()));
        config.put("web.http.identity.path", identityApi.getPath());
        config.put("web.http.sts.port", String.valueOf(sts.getPort()));
        config.put("web.http.sts.path", sts.getPath());
        config.put("web.http.accounts.port", String.valueOf(accountsApi.getPort()));
        config.put("web.http.accounts.path", accountsApi.getPath());
        config.put("web.http.did.port", String.valueOf(didApi.getPort()));
        config.put("web.http.did.path", didApi.getPath());
        config.put("edc.iam.did.web.use.https", "false");
        config.put("edc.api.accounts.key", "password");
        return config;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URI getSts() {
        return sts;
    }

    public URI getResolutionApi() {
        return resolutionApi;
    }

    public String didFor(String participantId) {
        return "did:web:" + URLEncoder.encode(didApi.getHost() + ":" + didApi.getPort(), StandardCharsets.UTF_8) + ":" + participantId;
    }

    public static class Builder {
        protected final IdentityHubParticipant participant;

        protected Builder() {
            this.participant = new IdentityHubParticipant();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.participant.id = id;
            return this;
        }

        public Builder name(String name) {
            this.participant.name = name;
            return this;
        }

        public IdentityHubParticipant build() {
            Objects.requireNonNull(this.participant.id, "id");
            Objects.requireNonNull(this.participant.name, "name");

            return this.participant;
        }
    }
}
