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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApiContext;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;

/**
 * Authenticates requests against the Proxmox VE API using an API token.
 *
 * Unlike the ticket based authentication, API tokens are stateless: the token is sent with every request via the
 * {@code Authorization} header and no CSRF prevention token is required for write requests. This is the recommended way
 * to authenticate automated clients such as this binding.
 *
 * @author Daniel Zupan - Initial contribution
 *
 * @see <a href="https://pve.proxmox.com/wiki/Proxmox_VE_API#API_Tokens">Proxmox VE API tokens</a>
 */
@NonNullByDefault
public class TokenAuthentication implements Authorization {

    private final ProxmoxVEApiContext context;

    public TokenAuthentication(ProxmoxVEApiContext context) {
        this.context = context;
    }

    @Override
    public void authenticate(Request request) throws ProxmoxApiConfigurationException {
        String tokenId = context.getConfig().getApiTokenId();
        String tokenSecret = context.getConfig().getApiTokenSecret();
        if (tokenId == null || tokenId.isBlank() || tokenSecret == null || tokenSecret.isBlank()) {
            throw new ProxmoxApiConfigurationException("API token id or secret is missing!");
        }

        request.header(HttpHeader.AUTHORIZATION, "PVEAPIToken=" + tokenId + "=" + tokenSecret);
    }
}
