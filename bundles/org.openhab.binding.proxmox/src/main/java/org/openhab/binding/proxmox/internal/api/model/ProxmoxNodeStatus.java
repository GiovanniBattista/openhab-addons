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
 * @author Daniel Zupan - Initial contribution
 *
 */
public class ProxmoxNodeStatus {
    @SerializedName("pveversion")
    private String pveVersion;
    @SerializedName("kversion")
    private String kVersion;
    private int uptime;

    /**
     * @return the kVersion
     */
    public String getkVersion() {
        return kVersion;
    }

    /**
     * @return the pveVersion
     */
    public String getPveVersion() {
        return pveVersion;
    }

    /**
     * @return the uptime
     */
    public int getUptime() {
        return uptime;
    }
}
