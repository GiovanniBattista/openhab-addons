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

import java.util.Objects;

/**
 * @author danie
 *
 */
public class AuthorizationDecisionResponse {

    private final AuthorizationDecisionResponseCode responseCode;
    private String apiKey;

    public AuthorizationDecisionResponse(AuthorizationDecisionResponseCode responseCode) {
        this.responseCode = Objects.requireNonNull(responseCode);
    }

    public AuthorizationDecisionResponse(AuthorizationDecisionResponseCode responseCode, String apiKey) {
        this(responseCode);
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    /**
     * @return the responseCode
     */
    public AuthorizationDecisionResponseCode getResponseCode() {
        return responseCode;
    }

    /**
     * @return the apiKey
     */
    public String getApiKey() {
        return apiKey;
    }
}
