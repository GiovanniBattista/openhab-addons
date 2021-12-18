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
 *
 */
public class OctoPrintApiFactory {
    public static OctoPrintApi create(OctoPrintConfiguration config, HttpClient httpClient) {

        Gson gson = new Gson();
        OctoPrintApiContext context = OctoPrintApiContext.of(config, httpClient, gson);

        return new OctoPrintApi(context);
    }
}
