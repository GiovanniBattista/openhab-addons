/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.proxmox.internal.action;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingHandler;

/**
 * Action for Proxmox Node to shutdown/restart node or
 * startAll/stopAll all VMs and LXCs.
 *
 * @author Daniel Zupan - Initial contribution
 */
public class ProxmoxNodeAction implements ThingActions {

    // TODO implement shutdown/restart of node via POST /status
    // TODO implement startAll/stopAll via POST /startAll /stopAll
    @Override
    public void setThingHandler(ThingHandler handler) {
        // TODO Auto-generated method stub
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        // TODO Auto-generated method stub
        return null;
    }
}
