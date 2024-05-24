/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.proxmox.internal.api.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This exception is thrown in case the configuration for the API is not correct
 * (i.e. missing username, password, baseUrl)
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxApiConfigurationException extends Exception {

    public ProxmoxApiConfigurationException(String message) {
        super(message);
    }

    public ProxmoxApiConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
