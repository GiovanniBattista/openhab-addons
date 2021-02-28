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
package org.openhab.binding.proxmox.internal.api;

import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;

import com.google.gson.JsonObject;

/**
 * Simple request builder for convenience
 *
 * @author Daniel Zupan - Initial contribution
 */
public class ProxmoxRequestHelper {

    private static final String API_BASE_PATH = "api2/json";

    private final ProxmoxVEApiContext context;
    private final String apiUrl;

    public static ProxmoxRequestHelper of(ProxmoxVEApiContext context) {
        String baseUrl = context.getConfig().getBaseUrl();

        StringBuilder apiUrlBuilder = new StringBuilder(context.getConfig().getBaseUrl());
        if (!baseUrl.endsWith("/")) {
            apiUrlBuilder.append("/");
        }
        apiUrlBuilder.append(API_BASE_PATH);

        return new ProxmoxRequestHelper(context, apiUrlBuilder.toString());
    }

    private ProxmoxRequestHelper(ProxmoxVEApiContext context, String apiUrl) {
        this.context = context;
        this.apiUrl = apiUrl;
    }

    /**
     * Makes a new request
     *
     * @param pathTemplate a path as {@link MessageFormat} template, like "/nodes/{0}/status"
     * @param pathTemplateValues "pve"
     * @return the new request
     */
    public Request newGetRequest(String pathTemplate, String... pathTemplateValues) {
        return newRequest(MessageFormat.format(pathTemplate, (Object[]) pathTemplateValues)).method(HttpMethod.GET);
    }

    public Request newPostRequest(String pathTemplate, String... pathTemplateValues) {
        return newRequest(MessageFormat.format(pathTemplate, (Object[]) pathTemplateValues)).method(HttpMethod.POST)
                .header("Content-Type", "application/json");
    }

    private Request newRequest(String path) {
        // TODO Make timeout configurable?
        String validPath = path;
        if (!path.startsWith("/")) {
            validPath = "/" + validPath;
        }
        return context.getHttpClient().newRequest(apiUrl + validPath).timeout(5000, TimeUnit.MILLISECONDS)
                .accept("application/json");
    }

    public <T> T getContent(Request request, Class<T> classToExtract) throws ProxmoxApiCommunicationException {
        T content;
        ContentResponse response;
        try {
            response = request.send();
        } catch (InterruptedException ex) {
            throw new ProxmoxApiCommunicationException("Request was interrupted", ex);
        } catch (TimeoutException ex) {
            throw new ProxmoxApiCommunicationException("Request - Timeout reached", ex);
        } catch (ExecutionException ex) {
            throw new ProxmoxApiCommunicationException("Request failed", ex);
        }

        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.OK_200) {
            JsonObject responseContainer = context.getGson().fromJson(response.getContentAsString(), JsonObject.class);
            if (responseContainer != null && responseContainer.has("data")) {
                content = context.getGson().fromJson(responseContainer.get("data"), classToExtract);
            } else {
                throw new ProxmoxApiCommunicationException("No content was provided in response");
            }
        } else {
            throw new ProxmoxApiCommunicationException(
                    "API call returned invalid status code. StatusCode=" + statusCode);
        }
        return content;
    }

    public <T> List<T> getContentAsList(Request request, Type collectionType) throws ProxmoxApiCommunicationException {
        List<T> content;
        ContentResponse response;
        try {
            response = request.send();
        } catch (InterruptedException ex) {
            throw new ProxmoxApiCommunicationException("Request was interrupted", ex);
        } catch (TimeoutException ex) {
            throw new ProxmoxApiCommunicationException("Request - Timeout reached", ex);
        } catch (ExecutionException ex) {
            throw new ProxmoxApiCommunicationException("Request failed", ex);
        }

        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.OK_200) {
            JsonObject responseContainer = context.getGson().fromJson(response.getContentAsString(), JsonObject.class);
            if (responseContainer != null && responseContainer.has("data")) {
                content = context.getGson().fromJson(responseContainer.get("data"), collectionType);
            } else {
                throw new ProxmoxApiCommunicationException("No content was provided in response");
            }
        } else {
            throw new ProxmoxApiCommunicationException(
                    "API call returned invalid status code. StatusCode=" + statusCode);
        }
        return content;
    }

    public void sendRequest(Request request) throws ProxmoxApiCommunicationException {
        ContentResponse response;
        try {
            response = request.send();
        } catch (InterruptedException ex) {
            throw new ProxmoxApiCommunicationException("Request was interrupted", ex);
        } catch (TimeoutException ex) {
            throw new ProxmoxApiCommunicationException("Request - Timeout reached", ex);
        } catch (ExecutionException ex) {
            throw new ProxmoxApiCommunicationException("Request failed", ex);
        }

        int statusCode = response.getStatus();
        if (statusCode != HttpStatus.OK_200) {
            throw new ProxmoxApiCommunicationException(
                    "API call returned invalid status code. StatusCode=" + statusCode);
        }
    }
}
