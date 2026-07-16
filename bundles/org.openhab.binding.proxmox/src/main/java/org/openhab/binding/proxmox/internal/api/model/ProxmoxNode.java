/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

/**
 * ProxmoxNode
 *
 * @author Daniel Zupan - Initial contribution
 */
public class ProxmoxNode {
    private String id;
    private String type;
    private String node;
    private NodeStatus status;
    private long maxmem;
    private long uptime;
    private long disk;
    private int maxcpu;
    private long maxdisk;
    private float cpu;
    private long mem;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getNode() {
        return node;
    }

    public NodeStatus getStatus() {
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
