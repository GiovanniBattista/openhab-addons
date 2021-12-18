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
package org.openhab.binding.octoprint.internal.api;

import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.octoprint.internal.OctoPrintConfiguration;

import com.google.gson.Gson;

/**
 * @author Daniel Zupan - Initial contribution
 */
public class OctoPrintApiContext {

    private final HttpClient httpClient;
    private final Gson gson;
    private OctoPrintConfiguration config;

    public static OctoPrintApiContext of(OctoPrintConfiguration config, HttpClient httpClient, Gson gson) {
        return new OctoPrintApiContext(config, httpClient, gson);
    }

    private OctoPrintApiContext(OctoPrintConfiguration config, HttpClient httpClient, Gson gson) {
        this.config = config;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * @return the config
     */
    public OctoPrintConfiguration getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(OctoPrintConfiguration config) {
        this.config = config;
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
