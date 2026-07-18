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
package org.openhab.binding.proxmox.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.proxmox.internal.api.auth.Authorization;
import org.openhab.binding.proxmox.internal.api.auth.ProxmoxAuthentication;
import org.openhab.binding.proxmox.internal.api.auth.TokenAuthentication;
import org.openhab.binding.proxmox.internal.config.ProxmoxHostConfiguration;

import com.google.gson.Gson;

/**
 * ProxmoxVEApiFactory
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxVEApiFactory {
    public static ProxmoxVEApi create(ProxmoxHostConfiguration config, HttpClient httpClient) {
        Gson gson = GsonBuilderFactory.defaultBuilder().create();
        ProxmoxVEApiContext context = ProxmoxVEApiContext.of(config, httpClient, gson);

        // Prefer stateless API token authentication over the ticket/password based one when a token is configured.
        Authorization auth = config.usesApiToken() ? new TokenAuthentication(context)
                : new ProxmoxAuthentication(context);
        return new ProxmoxVEApi(context, auth);
    }
}
