/*
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

import java.time.LocalDateTime;

import com.google.gson.annotations.SerializedName;

/**
 * Response of an AccessTicket
 *
 * @author Daniel Zupan - Initial contribution
 */
public class AccessTicketResponse {

    @SerializedName("CSRFPreventionToken")
    private String csrfPreventionToken;
    @SerializedName("clustername")
    private String clusterName;
    private String ticket;

    private transient final LocalDateTime tokenExpiration;

    public AccessTicketResponse() {
        super();

        // as stated here: https://pve.proxmox.com/wiki/Proxmox_VE_API#Authentication
        // "Tickets have a limited lifetime of 2 hours. But you can simple get a new ticket by passing the
        // old
        // ticket as password to the /access/ticket method."
        tokenExpiration = LocalDateTime.now().plusHours(2);
    }

    public String getCsrfPreventionToken() {
        return csrfPreventionToken;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getTicket() {
        return ticket;
    }

    public LocalDateTime getTokenExpiration() {
        return tokenExpiration;
    }
}
