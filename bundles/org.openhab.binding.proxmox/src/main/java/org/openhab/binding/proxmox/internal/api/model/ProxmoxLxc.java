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

import com.google.gson.annotations.SerializedName;

/**
 * ProxmoxLxc
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxLxc {
    private @Nullable VmStatus status;

    @SerializedName("vmid")
    private @Nullable String lxcId;
    private @Nullable String name;
    private @Nullable String tags;
    private int cpus;
    private float cpu;
    private long mem;
    @SerializedName("maxmem")
    private long maxMem;
    @SerializedName("maxswap")
    private long maxSwap;
    private long disk;
    @SerializedName("maxdisk")
    private long maxDisk;
    private long uptime;

    /**
     * @return the status
     */
    public @Nullable VmStatus getStatus() {
        return status;
    }

    /**
     * @return the lxcId
     */
    public @Nullable String getLxcId() {
        return lxcId;
    }

    /**
     * @return the number of assigned cpu cores
     */
    public int getCpus() {
        return cpus;
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
    public long getMaxMem() {
        return maxMem;
    }

    /**
     * @return the maximum available swap in bytes
     */
    public long getMaxSwap() {
        return maxSwap;
    }

    /**
     * @return the currently used disk size in bytes
     */
    public long getDisk() {
        return disk;
    }

    /**
     * @return the maximum available disk size in bytes
     */
    public long getMaxDisk() {
        return maxDisk;
    }

    /**
     * @return the name
     */
    public @Nullable String getName() {
        return name;
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
}
