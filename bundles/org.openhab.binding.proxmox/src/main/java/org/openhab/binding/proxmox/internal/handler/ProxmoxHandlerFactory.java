/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import static org.openhab.binding.proxmox.internal.ProxmoxBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ProxmoxHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.proxmox", service = ThingHandlerFactory.class)
public class ProxmoxHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_HOST, THING_TYPE_NODE,
            THING_TYPE_VM, THING_TYPE_LXC);

    private final HttpClientFactory httpClientFactory;

    @Activate
    public ProxmoxHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_HOST.equals(thingTypeUID)) {
            return new ProxmoxHostBridgeHandler((Bridge) thing, httpClientFactory);
        } else if (THING_TYPE_NODE.equals(thingTypeUID)) {
            return new ProxmoxNodeHandler(thing);
        } else if (THING_TYPE_VM.equals(thingTypeUID)) {
            return new ProxmoxVmHandler(thing);
        } else if (THING_TYPE_LXC.equals(thingTypeUID)) {
            return new ProxmoxLxcHandler(thing);
        }

        return null;
    }
}
