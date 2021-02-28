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
package org.openhab.binding.proxmox.internal.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author danie
 *
 */
public class ProxmoxLxc {
    private VmStatus status;

    @SerializedName("vmid")
    private String lxcId;
    private int cpus;
    @SerializedName("maxdisk")
    private long maxDisk;
    @SerializedName("maxmem")
    private int maxMem;
    @SerializedName("maxswap")
    private int maxSwap;
    private String name;
    private String tags;
    private int uptime;

    /**
     * @return the status
     */
    public VmStatus getStatus() {
        return status;
    }

    /**
     * @return the lxcId
     */
    public String getLxcId() {
        return lxcId;
    }

    /**
     * @return the cpus
     */
    public int getCpus() {
        return cpus;
    }

    /**
     * @return the maxDisk
     */
    public long getMaxDisk() {
        return maxDisk;
    }

    /**
     * @return the maxMem
     */
    public int getMaxMem() {
        return maxMem;
    }

    /**
     * @return the maxSwap
     */
    public int getMaxSwap() {
        return maxSwap;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * @return the uptime
     */
    public int getUptime() {
        return uptime;
    }
}
