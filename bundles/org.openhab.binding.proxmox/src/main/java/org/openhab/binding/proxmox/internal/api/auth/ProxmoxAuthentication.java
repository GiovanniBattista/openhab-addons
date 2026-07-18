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
package org.openhab.binding.proxmox.internal.api.auth;

import java.net.HttpCookie;
import java.time.LocalDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;
import org.openhab.binding.proxmox.internal.api.ProxmoxRequestHelper;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApiContext;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.AccessTicketResponse;

/**
 * Authenticates requests against the Proxmox VE API using the ticket/cookie based authentication.
 *
 * @author Daniel Zupan - Initial contribution
 *
 * @see <a href="https://pve.proxmox.com/wiki/Proxmox_VE_API#Authentication">Proxmox VE API authentication</a>
 */
@NonNullByDefault
public class ProxmoxAuthentication implements Authorization {

    private final ProxmoxVEApiContext context;
    private final ProxmoxRequestHelper requestHelper;

    private @Nullable AccessTicketResponse accessTicket;

    public ProxmoxAuthentication(ProxmoxVEApiContext context) {
        this.context = context;
        this.requestHelper = ProxmoxRequestHelper.of(context);
    }

    /**
     * Adds the authentication ticket as a cookie to the given request and, for write requests, the required
     * CSRF prevention token header.
     */
    @Override
    public synchronized void authenticate(Request request)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        AccessTicketResponse ticket = getValidTicket();

        String ticketValue = ticket.getTicket();
        if (ticketValue == null) {
            throw new ProxmoxApiCommunicationException("Received an empty authentication ticket");
        }
        request.cookie(new HttpCookie("PVEAuthCookie", ticketValue));

        if (isWriteRequest(request)) {
            // any write request must include the CSRFPreventionToken header
            String csrfPreventionToken = ticket.getCsrfPreventionToken();
            if (csrfPreventionToken != null) {
                request.header("CSRFPreventionToken", csrfPreventionToken);
            }
        }
    }

    private AccessTicketResponse getValidTicket()
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        AccessTicketResponse ticket = accessTicket;
        if (ticket == null || isExpired(ticket)) {
            ticket = initializeTokens();
        }
        return ticket;
    }

    private AccessTicketResponse initializeTokens()
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        validateConfiguration();

        Fields fields = new Fields();
        fields.put("username", context.getConfig().getUsername());
        fields.put("password", context.getConfig().getPassword());

        Request request = requestHelper.newPostRequest("/access/ticket").content(new FormContentProvider(fields));

        AccessTicketResponse ticket = requestHelper.getContent(request, AccessTicketResponse.class);
        accessTicket = ticket;
        return ticket;
    }

    private void validateConfiguration() throws ProxmoxApiConfigurationException {
        String baseUrl = context.getConfig().getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new ProxmoxApiConfigurationException("Base URL is missing!");
        }
        String username = context.getConfig().getUsername();
        if (username == null || username.isEmpty()) {
            throw new ProxmoxApiConfigurationException("No username was provided!");
        }
        String password = context.getConfig().getPassword();
        if (password == null || password.isEmpty()) {
            throw new ProxmoxApiConfigurationException("No password was provided!");
        }
    }

    private boolean isExpired(AccessTicketResponse ticket) {
        return ticket.getTokenExpiration().isBefore(LocalDateTime.now());
    }

    private boolean isWriteRequest(Request request) {
        return HttpMethod.POST.is(request.getMethod()) || HttpMethod.PUT.is(request.getMethod())
                || HttpMethod.DELETE.is(request.getMethod());
    }
}
