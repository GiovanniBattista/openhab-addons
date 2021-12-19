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
package org.openhab.binding.octoprint.internal.api.model;

/**
 * @author Daniel Zupan
 *
 */
public class SystemCommand {

    private String action;
    private String name;
    private String confirm;
    private String source;

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the confirm
     */
    public String getConfirm() {
        return confirm;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

}
