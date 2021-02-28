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
package org.openhab.binding.proxmox.internal.api.exception;

/**
 * This exception is thrown in case of an communication error with the Proxmox VE API.
 *
 * @author Daniel Zupan - Initial contribution
 */
public class ProxmoxApiCommunicationException extends Exception {

    public ProxmoxApiCommunicationException(String message) {
        super(message);
    }

    public ProxmoxApiCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
