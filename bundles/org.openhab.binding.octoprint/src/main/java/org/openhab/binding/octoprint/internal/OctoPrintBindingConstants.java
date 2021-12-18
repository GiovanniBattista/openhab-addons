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
package org.openhab.binding.octoprint.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link OctoPrintBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class OctoPrintBindingConstants {

    private static final String BINDING_ID = "octoprint";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_OCTOPRINT = new ThingTypeUID(BINDING_ID, "octoprint");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_OCTOPRINT).collect(Collectors.toSet()));

    // Config parameters
    public static final String HOST = "hostname";
    public static final String PORT = "port";
    public static final String API_VERSION = "apiVersion";
    public static final String API_KEY = "apiKey";
    public static final String PATH = "path";
    public static final String UUID = "uuid";
    public static final String OCTOPRINT_VERSION = "octoprintVersion";

    // List of all Channel ids
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_CURRENT_TEMPERATURE_TOOL0 = "current-temperature-tool0";
    public static final String CHANNEL_TARGET_TEMPERATURE_TOOL0 = "target-temperature-tool0";
    public static final String CHANNEL_CURRENT_TEMPERATURE_BED = "current-temperature-bed";
    public static final String CHANNEL_TARGET_TEMPERATURE_BED = "target-temperature-bed";
}
