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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * @author Daniel Zupan - Initial contribution
 */
public enum VmStatus {
    @SerializedName("stopped")
    STOPPED("stopped"),

    @SerializedName("running")
    RUNNING("running");

    private final String value;

    private VmStatus(String value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
