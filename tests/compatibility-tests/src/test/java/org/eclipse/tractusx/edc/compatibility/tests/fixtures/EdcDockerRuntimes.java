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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.yaml.snakeyaml.util.Tuple;

import java.util.Map;

public enum EdcDockerRuntimes {

    STABLE_CONNECTOR_0_7_6("controlplane-076:latest", "dataplane-076:latest"),
    STABLE_CONNECTOR("controlplane-stable:latest", "dataplane-stable:latest");


    private final String controlPlaneImage;
    private final String dataPlaneImage;


    EdcDockerRuntimes(String controlPlaneImage, String dataPlaneImage) {
        this.controlPlaneImage = controlPlaneImage;
        this.dataPlaneImage = dataPlaneImage;
    }

    public Tuple<GenericContainer<?>, GenericContainer<?>> start(Map<String, String> controlPlaneEnv, Map<String, String> dataPlaneEnv) {
        var controlPlane =
                new GenericContainer<>(controlPlaneImage).withCreateContainerCmdModifier(cmd -> cmd.withName(this.name() + "-controlplane")).withNetworkMode("host").waitingFor(Wait.forLogMessage(".*Runtime .* ready.*", 1)).withEnv(controlPlaneEnv);
        var dataPlane = new GenericContainer<>(dataPlaneImage).withCreateContainerCmdModifier(cmd -> cmd.withName(this.name() + "-dataplane")).withNetworkMode("host").waitingFor(Wait.forLogMessage(".*Runtime .* ready.*", 1)).withEnv(dataPlaneEnv);
        controlPlane.start();
        dataPlane.start();
        return new Tuple<>(controlPlane, dataPlane);
    }

}
