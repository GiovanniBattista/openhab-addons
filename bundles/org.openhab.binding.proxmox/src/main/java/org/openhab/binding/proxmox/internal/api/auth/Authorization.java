/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;

/**
 * Authorization
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public interface Authorization {

    void authenticate(Request request) throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException;
}
