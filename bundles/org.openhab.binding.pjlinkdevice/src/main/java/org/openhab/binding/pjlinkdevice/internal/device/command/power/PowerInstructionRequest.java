/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.pjlinkdevice.internal.device.command.power;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.pjlinkdevice.internal.device.command.Request;

/**
 * The request part of {@link PowerInstructionCommand}
 *
 * @author Nils Schnabel - Initial contribution
 */
@NonNullByDefault
public class PowerInstructionRequest implements Request {

    private PowerInstructionCommand command;

    public PowerInstructionRequest(PowerInstructionCommand command) {
        this.command = command;
    }

    @Override
    public String getRequestString() {
        return "%1POWR " + this.command.getTarget().getPJLinkRepresentation();
    }
}
