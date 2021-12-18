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
package org.openhab.binding.octoprint.internal.discovery;

import static org.openhab.binding.octoprint.internal.OctoPrintBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Zupan - Initial contribution
 *
 */
@Component
@NonNullByDefault
public class OctoPrintDiscoveryParticipant implements MDNSDiscoveryParticipant {

    // private final Logger logger = LoggerFactory.getLogger(OctoPrintDiscoveryParticipant.class);

    private static final String SERVICE_TYPE = "_octoprint._tcp.local.";

    private static final String PROPERTY_API_VERSION = "api";
    private static final String PROPERTY_PATH = "path";
    private static final String PROPERTY_UUID = "uuid";
    private static final String PROPERTY_OCTOPRINT_VERSION = "version";

    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }

    @Override
    public Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo service) {
        final ThingUID uid = getThingUID(service);
        if (uid == null) {
            return null;
        }

        String host = service.getHostAddresses()[0];
        int port = service.getPort();

        String apiVersion = service.getPropertyString(PROPERTY_API_VERSION);
        String path = service.getPropertyString(PROPERTY_PATH);
        String uuid = service.getPropertyString(PROPERTY_UUID);
        String octoprintVersion = service.getPropertyString(PROPERTY_OCTOPRINT_VERSION);

        final Map<String, Object> properties = new HashMap<>();
        properties.put(HOST, host);
        properties.put(PORT, port);
        properties.put(API_VERSION, apiVersion);
        properties.put(PATH, path);
        properties.put(UUID, uuid);
        properties.put(OCTOPRINT_VERSION, octoprintVersion);

        return DiscoveryResultBuilder.create(uid).withThingType(getThingType(service)).withProperties(properties)
                .withRepresentationProperty(UUID).withLabel(service.getName()).build();
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        ThingTypeUID thingTypeUID = getThingType(service);
        if (thingTypeUID != null) {
            String uuid = service.getPropertyString(PROPERTY_UUID);
            if (uuid != null) {
                return new ThingUID(THING_TYPE_OCTOPRINT, uuid);
            }
        }
        return null;
    }

    private @Nullable ThingTypeUID getThingType(final ServiceInfo service) {
        return THING_TYPE_OCTOPRINT; // we only have one thing type at the moment
    }
}
