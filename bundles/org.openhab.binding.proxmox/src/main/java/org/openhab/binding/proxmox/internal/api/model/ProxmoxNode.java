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
package org.openhab.binding.proxmox.internal.api.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * ProxmoxNode
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxNode {
    private @Nullable String id;
    private @Nullable String type;
    private @Nullable String node;
    private @Nullable NodeStatus status;
    private long maxmem;
    private long uptime;
    private long disk;
    private int maxcpu;
    private long maxdisk;
    private float cpu;
    private long mem;

    public @Nullable String getId() {
        return id;
    }

    public @Nullable String getType() {
        return type;
    }

    public @Nullable String getNode() {
        return node;
    }

    public @Nullable NodeStatus getStatus() {
        return status;
    }

    public long getMaxmem() {
        return maxmem;
    }

    public long getUptime() {
        return uptime;
    }

    public long getDisk() {
        return disk;
    }

    public int getMaxcpu() {
        return maxcpu;
    }

    public long getMaxdisk() {
        return maxdisk;
    }

    public float getCpu() {
        return cpu;
    }

    public long getMem() {
        return mem;
    }
}
