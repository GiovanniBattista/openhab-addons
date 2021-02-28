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
package org.openhab.binding.proxmox.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link ProxmoxBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxBindingConstants {

    private static final String BINDING_ID = "proxmox";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_HOST = new ThingTypeUID(BINDING_ID, "host");
    public static final ThingTypeUID THING_TYPE_NODE = new ThingTypeUID(BINDING_ID, "node");
    public static final ThingTypeUID THING_TYPE_VM = new ThingTypeUID(BINDING_ID, "vm");
    public static final ThingTypeUID THING_TYPE_LXC = new ThingTypeUID(BINDING_ID, "lxc");

    // List of all Channel ids
    public static final String CHANNEL_POWER = "power";

    // List of all Properties
    public static final String PROPERTY_HOST_VERSION = "version";

    public static final String PROPERTY_NODE_NAME = "name";
    public static final String PROPERTY_NODE_TYPE = "type";

    public static final String PROPERTY_VM_ID = "id";
    public static final String PROPERTY_VM_NODE = "node";

    public static final String PROPERTY_LXC_ID = "id";
    public static final String PROPERTY_LXC_NODE = "node";
}
