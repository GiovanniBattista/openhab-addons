package org.openhab.binding.proxmox.internal.api.model;

import java.time.LocalDateTime;

import com.google.gson.annotations.SerializedName;

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
