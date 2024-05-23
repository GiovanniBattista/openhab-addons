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

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.nuki.internal.constants.NukiWebApiLinkBuilder;
import org.openhab.binding.nuki.internal.dataexchange.NukiBaseResponse;
import org.openhab.binding.nuki.internal.webapi.dto.WebApiAccount;
import org.openhab.binding.nuki.internal.webapi.dto.WebApiSmartLockDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * @author Daniel Zupan - Initial contribution
 */
public class NukiWebApiHttpClient {
    private final Logger logger = LoggerFactory.getLogger(NukiWebApiHttpClient.class);

    private final HttpClient httpClient;
    private final String apiToken;
    private final Gson gson;
    private final NukiWebApiLinkBuilder linkBuilder;

    public NukiWebApiHttpClient(HttpClient httpClient, String apiToken, NukiWebApiLinkBuilder linkBuilder) {
        logger.debug("Instantiating NukiHttpClient");
        this.httpClient = httpClient;
        this.apiToken = apiToken;
        this.linkBuilder = linkBuilder;
        gson = new Gson();
    }

    public WebApiAccountListResponse getAccounts() {
        logger.debug("getAccounts() in thread {}", Thread.currentThread().getId());
        try {
            ContentResponse contentResponse = executeRequest(linkBuilder.getSmartLocks());
            int status = contentResponse.getStatus();
            String response = contentResponse.getContentAsString();
            logger.debug("getSmartLocks status[{}] response[{}]", status, response);
            if (status == HttpStatus.OK_200) {
                WebApiAccount[] accounts = gson.fromJson(response, WebApiAccount[].class);
                return new WebApiAccountListResponse(status, contentResponse.getReason(), Arrays.asList(accounts));
            } else {
                logger.debug("Could not get smart locks! Status[{}] - Response[{}]", status, response);
                return new WebApiAccountListResponse(status, contentResponse.getReason(), null);
            }
        } catch (Exception e) {
            logger.debug("Could not get List! Exception[{}]", e.getMessage());
            return new WebApiAccountListResponse(handleException(e));
        }
    }

    public WebApiSmartLockListResponse getSmartLocks() {
        logger.debug("getSmartLocks() in thread {}", Thread.currentThread().getId());
        try {
            ContentResponse contentResponse = executeRequest(linkBuilder.getSmartLocks());
            int status = contentResponse.getStatus();
            String response = contentResponse.getContentAsString();
            logger.debug("getSmartLocks status[{}] response[{}]", status, response);
            if (status == HttpStatus.OK_200) {
                WebApiSmartLockDevice[] smartLocks = gson.fromJson(response, WebApiSmartLockDevice[].class);
                return new WebApiSmartLockListResponse(status, contentResponse.getReason(), Arrays.asList(smartLocks));
            } else {
                logger.debug("Could not get smart locks! Status[{}] - Response[{}]", status, response);
                return new WebApiSmartLockListResponse(status, contentResponse.getReason(), null);
            }
        } catch (Exception e) {
            logger.debug("Could not get List! Exception[{}]", e.getMessage());
            return new WebApiSmartLockListResponse(handleException(e));
        }
    }

    private synchronized ContentResponse executeRequest(URI uri)
            throws InterruptedException, ExecutionException, TimeoutException {
        logger.debug("executeRequest({})", uri);
        ContentResponse contentResponse = httpClient.newRequest(uri).method(HttpMethod.GET)
                .header(HttpHeader.AUTHORIZATION, "Bearer " + apiToken).send();
        logger.debug("contentResponseAsString[{}]", contentResponse.getContentAsString());
        return contentResponse;
    }

    private NukiBaseResponse handleException(Exception e) {
        if (e instanceof ExecutionException) {
            Throwable cause = e.getCause();
            if (cause instanceof HttpResponseException) {
                HttpResponseException causeException = (HttpResponseException) cause;
                int status = causeException.getResponse().getStatus();
                String reason = causeException.getResponse().getReason();
                logger.debug("HTTP Response Exception! Status[{}] - Reason[{}]! Check your API Token!", status, reason);
                return new NukiBaseResponse(status, reason);
            }
        }
        logger.error("Could not handle Exception! Exception[{}]", e.getMessage(), e);
        return new NukiBaseResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
    }
}
