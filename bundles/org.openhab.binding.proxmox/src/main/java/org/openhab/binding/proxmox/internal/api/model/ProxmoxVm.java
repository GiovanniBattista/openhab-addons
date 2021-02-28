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

/**
 * @author Daniel Zupan - Initial contribution
 *
 */
public class ProxmoxVm {
    private VmStatus status;
    private String vmid;
    private String name;
    private int pid;
    private String tags;
    private int uptime;

    /**
     * @return the status
     */
    public VmStatus getStatus() {
        return status;
    }

    /**
     * @return the vmid
     */
    public String getVmid() {
        return vmid;
    }

    /**
     * @return the name
     */
    public String getName() {
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
