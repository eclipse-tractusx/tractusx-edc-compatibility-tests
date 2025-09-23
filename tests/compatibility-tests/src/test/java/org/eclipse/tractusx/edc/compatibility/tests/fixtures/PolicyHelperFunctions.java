/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests.fixtures;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.policy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;

public class PolicyHelperFunctions {

    private static final String MEMBERSHIP_LITERAL = CX_POLICY_NS + "Membership";

    public static JsonObject contractExpiresIn(String offset) {
        return inForceDatePolicyLegacy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+" + offset);
    }

    private static JsonObject membershipConstraint() {
        return Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add("leftOperand", MEMBERSHIP_LITERAL)
                .add("operator", "eq")
                .add("rightOperand", "active")
                .build();
    }

    private static JsonObject inForceDatePolicyLegacy(String operatorStart, Object startDate, String operatorEnd, Object endDate) {
        var constraint = Json.createObjectBuilder()
                .add("@type", "LogicalConstraint")
                .add("and", Json.createArrayBuilder()
                        .add(atomicConstraint("https://w3id.org/edc/v0.0.1/ns/inForceDate", operatorStart, startDate, false))
                        .add(atomicConstraint("https://w3id.org/edc/v0.0.1/ns/inForceDate", operatorEnd, endDate, false))
                        .add(membershipConstraint())
                        .build())
                .build();

        return policy(List.of(Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", constraint)
                .build()));
    }


    private static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand, boolean createRightOperandsAsArray) {
        var builder = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add("leftOperand", leftOperand)
                .add("operator", operator);

        if (rightOperand instanceof Collection<?> coll && createRightOperandsAsArray) {
            builder.add("rightOperand", coll.stream()
                    .map(Object::toString)
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                    .build());
        } else if (rightOperand instanceof Collection<?> coll) {
            builder.add("rightOperand", coll.stream().map(Object::toString).collect(Collectors.joining(",")));
        } else {
            builder.add("rightOperand", rightOperand.toString());
        }
        return builder.build();
    }
}
