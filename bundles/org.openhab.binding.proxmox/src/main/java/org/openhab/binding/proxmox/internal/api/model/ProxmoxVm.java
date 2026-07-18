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
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxVm {
    private @Nullable VmStatus status;
    private @Nullable String vmid;
    private @Nullable String name;
    private @Nullable String tags;
    private int pid;
    private long uptime;
    private float cpu;
    private long mem;
    private long maxmem;
    private long maxdisk;

    /**
     * @return the status
     */
    public @Nullable VmStatus getStatus() {
        return status;
    }

    /**
     * @return the vmid
     */
    public @Nullable String getVmid() {
        return vmid;
    }

    /**
     * @return the name
     */
    public @Nullable String getName() {
        return name;
    }

    /**
     * @return the pid
     */
    public int getPid() {
        return pid;
    }

    /**
     * @return the tags
     */
    public @Nullable String getTags() {
        return tags;
    }

    /**
     * @return the uptime in seconds
     */
    public long getUptime() {
        return uptime;
    }

    /**
     * @return the current CPU usage as a fraction (0..1) of the assigned cores
     */
    public float getCpu() {
        return cpu;
    }

    /**
     * @return the currently used memory in bytes
     */
    public long getMem() {
        return mem;
    }

    /**
     * @return the maximum available memory in bytes
     */
    public long getMaxmem() {
        return maxmem;
    }

    /**
     * @return the maximum available disk size in bytes
     */
    public long getMaxdisk() {
        return maxdisk;
    }
}
