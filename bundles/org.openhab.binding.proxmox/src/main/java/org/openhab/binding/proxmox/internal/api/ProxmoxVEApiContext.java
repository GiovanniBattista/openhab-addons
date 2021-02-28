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
package org.openhab.binding.proxmox.internal.api;

import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.proxmox.internal.config.ProxmoxHostConfiguration;

import com.google.gson.Gson;

/**
 * @author Daniel Zupan - Initial contribution
 */
public class ProxmoxVEApiContext {

    private final ProxmoxHostConfiguration config;
    private final HttpClient httpClient;
    private final Gson gson;

    public static ProxmoxVEApiContext of(ProxmoxHostConfiguration config, HttpClient httpClient, Gson gson) {
        return new ProxmoxVEApiContext(config, httpClient, gson);
    }

    private ProxmoxVEApiContext(ProxmoxHostConfiguration config, HttpClient httpClient, Gson gson) {
        this.config = config;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * @return the config
     */
    public ProxmoxHostConfiguration getConfig() {
        return config;
    }

    /**
     * @return the httpClient
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * @return the gson
     */
    public Gson getGson() {
        return gson;
    }
}
