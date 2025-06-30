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
    private int maxmem;
    private int uptime;
    private int disk;
    private int maxcpu;
    private int maxdisk;
    private float cpu;
    private int mem;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the node
     */
    public String getNode() {
        return node;
    }

    /**
     * @return the status
     */
    public NodeStatus getStatus() {
        return status;
    }

    /**
     * @return the maxmem
     */
    public int getMaxmem() {
        return maxmem;
    }

    /**
     * @return the uptime
     */
    public int getUptime() {
        return uptime;
    }

    /**
     * @return the disk
     */
    public int getDisk() {
        return disk;
    }

    /**
     * @return the maxcpu
     */
    public int getMaxcpu() {
        return maxcpu;
    }

    /**
     * @return the maxdisk
     */
    public int getMaxdisk() {
        return maxdisk;
    }

    /**
     * @return the cpu
     */
    public float getCpu() {
        return cpu;
    }

    /**
     * @return the mem
     */
    public int getMem() {
        return mem;
    }
}
