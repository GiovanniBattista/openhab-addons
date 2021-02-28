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
package org.openhab.binding.proxmox.internal.handler;

/**
 * This is a listener that reacts on the various events of a Proxmox appliance,
 * e.g. a Node/VM/LXC was added/removed/changed.
 *
 * @author Daniel Zupan - Initial contribution
 */
public interface ProxmoxStatusChangedListener<T> {

    boolean onStateChanged(T appliance);

    void onAdded(T appliance);

    void onRemoved();

    void onGone();
}
