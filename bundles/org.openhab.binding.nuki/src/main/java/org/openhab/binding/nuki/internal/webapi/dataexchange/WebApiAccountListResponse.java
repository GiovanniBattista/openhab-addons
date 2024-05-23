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
package org.openhab.binding.nuki.internal.webapi.dataexchange;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nuki.internal.dataexchange.NukiBaseResponse;
import org.openhab.binding.nuki.internal.webapi.dto.WebApiAccount;

/**
 * @author Daniel Zupan - Initial contribution
 */
public class WebApiAccountListResponse extends NukiBaseResponse {
    private List<WebApiAccount> accounts;

    public WebApiAccountListResponse(int status, @Nullable String message, @Nullable List<WebApiAccount> accounts) {
        super(status, message);
        setSuccess(accounts != null);
        this.accounts = accounts == null ? Collections.emptyList() : Collections.unmodifiableList(accounts);
    }

    public WebApiAccountListResponse(NukiBaseResponse nukiBaseResponse) {
        this(nukiBaseResponse.getStatus(), nukiBaseResponse.getMessage(), null);
    }

    /**
     * @return the accounts
     */
    public List<WebApiAccount> getAccounts() {
        return accounts;
    }
}
