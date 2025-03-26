/*******************************************************************************
 * Copyright (c) 2025 Cofinity-X
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

import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

public class RemoteParticipantExtension implements BeforeAllCallback, AfterAllCallback {

    private final RemoteParticipant participant;
    private final LocalParticipant localParticipant;
    private final PostgresqlEndToEndExtension postgresql;

    private GenericContainer<?> controlPlane;
    private GenericContainer<?> dataPlane;

    public RemoteParticipantExtension(RemoteParticipant participant, LocalParticipant localParticipant, PostgresqlEndToEndExtension postgresql) {
        this.participant = participant;
        this.localParticipant = localParticipant;
        this.postgresql = postgresql;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        controlPlane = EdcDockerRuntimes.CONTROL_PLANE.create("remote-controlplane", participant.controlPlaneEnv(localParticipant, postgresql));
        dataPlane = EdcDockerRuntimes.DATA_PLANE.create("remote-dataplane", participant.dataPlaneEnv(localParticipant, postgresql));

        controlPlane.start();
        dataPlane.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (controlPlane != null) {
            controlPlane.stop();
        }
        if (dataPlane != null) {
            dataPlane.stop();
        }
    }
}
