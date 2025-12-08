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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.security.Vault;

import java.time.Instant;
import java.util.Base64;

public class DcpHelperFunctions {

    public static JsonObject createVc(String issuer, String type, JsonObject subjectSupplier) {
        return createVcBuilder(issuer, type, subjectSupplier)
                .build();
    }

    public static JsonObjectBuilder createVcBuilder(String issuer, String type, JsonObject subjectSupplier) {
        return Json.createObjectBuilder()
                .add("@context", context())
                .add("type", types(type))
                .add("credentialSubject", subjectSupplier)
                .add("issuer", issuer)
                .add("issuanceDate", Instant.now().toString());
    }

    public static JsonObject membershipSubject(String did, String id) {
        return Json.createObjectBuilder()
                .add("holderIdentifier", id)
                .add("id", did)
                .add("memberOf", "Catena-X")
                .build();

    }

    public static JsonObject bpnSubject(String did, String id) {
        return Json.createObjectBuilder()
                .add("holderIdentifier", id)
                .add("id", did)
                .add("bpn", id)
                .build();

    }

    public static JsonObject frameworkAgreementSubject(String did, String id) {
        return Json.createObjectBuilder()
                .add("type", "DataExchangeGovernanceCredential")
                .add("holderIdentifier", id)
                .add("contractVersion", "1.0.0")
                .add("contractTemplate", "https://public.catena-x.org/contracts/traceabilty.v1.pdf")
                .add("id", did)
                .build();

    }

    private static JsonArray types(String type) {
        return Json.createArrayBuilder()
                .add("VerifiableCredential")
                .add(type)
                .build();
    }

    private static JsonArray context() {
        return Json.createArrayBuilder()
                .add("https://www.w3.org/2018/credentials/v1")
                .add("https://w3id.org/security/suites/jws-2020/v1")
                .add("https://w3id.org/catenax/credentials")
                .add("https://w3id.org/vc/status-list/2021/v1")
                .build();
    }


    public static void configureParticipantContext(BaseParticipant participant, IdentityHubParticipant identityHubParticipant, RuntimeExtension identityHubRuntime) {
        var participantContextService = identityHubRuntime.getService(ParticipantContextService.class);

        var participantKey = participant.getKeyPairJwk();
        var key = KeyDescriptor.Builder.newInstance()
                .keyId(participant.getFullKeyId())
                .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                .privateKeyAlias(participant.getPrivateKeyAlias())
                .build();

        var service = new Service();
        service.setId("#credential-service");
        service.setType("CredentialService");
        service.setServiceEndpoint(identityHubParticipant.getResolutionApi() + "/v1/participants/" + toBase64(participant.getDid()));

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantContextId(participant.getDid())
                .did(participant.getDid())
                .key(key)
                .serviceEndpoint(service)
                .active(true)
                .build();

        participantContextService.createParticipantContext(participantManifest);

        var vault = identityHubRuntime.getService(Vault.class);
        vault.storeSecret(participant.getPrivateKeyAlias(), participant.getPrivateKeyAsString());
    }

    public static void configureParticipant(BaseParticipant participant, DataspaceIssuer issuer, IdentityHubParticipant identityHubParticipant, RuntimeExtension identityHubRuntime) {
        configureParticipantContext(participant, identityHubParticipant, identityHubRuntime);

        var accountService = identityHubRuntime.getService(StsAccountService.class);
        var vault = identityHubRuntime.getService(Vault.class);
        var credentialStore = identityHubRuntime.getService(CredentialStore.class);

        var credentials = issuer.issueCredentials(participant);

        credentials.forEach(credentialStore::create);

        accountService.findById(participant.getDid())
                .onSuccess(account -> {
                    vault.storeSecret(account.getSecretAlias(), "clientSecret");
                });

    }

    static String toBase64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

}
