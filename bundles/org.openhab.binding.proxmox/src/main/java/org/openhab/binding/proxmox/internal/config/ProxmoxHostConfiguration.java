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
package org.openhab.binding.proxmox.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * ProxmoxHostConfiguration
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxHostConfiguration {
    private @Nullable String baseUrl;
    private @Nullable String username;
    private @Nullable String password;
    private @Nullable String macAddress;

    private int pollingInterval = 30;

    /**
     * @return the baseUrl
     */
    public @Nullable String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @return the username
     */
    public @Nullable String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public @Nullable String getPassword() {
        return password;
    }

    /**
     * @param macAddress the macAddress to set
     */
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * @return the macAddress
     */
    public @Nullable String getMacAddress() {
        return macAddress;
    }

    /**
     * @return the pollingInterval
     */
    public int getPollingInterval() {
        return pollingInterval;
    }
}
