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
package org.openhab.binding.proxmox.internal.api.model;

/**
 * @author Daniel Zupan - Initial contribution
 */
public class ProxmoxVersion {
    private String release;
    private String repoid;
    private String version;

    /**
     * @return the release
     */
    public String getRelease() {
        return release;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the repoid
     */
    public String getRepoid() {
        return repoid;
    }
}
