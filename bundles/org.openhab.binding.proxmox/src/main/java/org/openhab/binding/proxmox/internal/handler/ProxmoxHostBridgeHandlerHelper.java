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
package org.openhab.binding.proxmox.internal.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApi;
import org.openhab.core.thing.Bridge;

/**
 * @author Daniel Zupan - Initial contribution
 *
 */
public class ProxmoxHostBridgeHandlerHelper {

    public static @Nullable ProxmoxHostBridgeHandler getBridgeHandler(@Nullable Bridge bridge) {
        ProxmoxHostBridgeHandler bridgeHandler = null;
        if (bridge != null && bridge.getHandler() instanceof ProxmoxHostBridgeHandler) {
            bridgeHandler = (ProxmoxHostBridgeHandler) bridge.getHandler();
        }
        return bridgeHandler;
    }

    public static @Nullable ProxmoxVEApi getApi(@Nullable Bridge bridge) {
        ProxmoxVEApi api = null;
        ProxmoxHostBridgeHandler bridgeHandler = getBridgeHandler(bridge);
        if (bridgeHandler != null) {
            api = bridgeHandler.getApi();
        }
        return api;
    }
}
