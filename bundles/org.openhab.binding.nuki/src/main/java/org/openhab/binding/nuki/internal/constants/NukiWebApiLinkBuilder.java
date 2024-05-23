/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.nuki.internal.constants;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class NukiWebApiLinkBuilder {
    private static final String WEB_API = "http://api.nuki.io";
    private static final String SMARTLOCK = "/smartlock";

    public NukiWebApiLinkBuilder() {
    }

    public URI getSmartLocks() {
        return builder(SMARTLOCK).build();
    }

    /**
     * @param smartlock2
     * @return
     */
    private UriBuilder builder(String path) {
        return UriBuilder.fromUri(WEB_API).path(path);
    }
}
