/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Simple request builder for convenience.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxRequestHelper {

    private static final String API_BASE_PATH = "api2/json";
    private static final int REQUEST_TIMEOUT_MILLIS = 5000;

    private final ProxmoxVEApiContext context;
    private final String apiUrl;

    public static ProxmoxRequestHelper of(ProxmoxVEApiContext context) {
        String baseUrl = context.getConfig().getBaseUrl();
        if (baseUrl == null) {
            baseUrl = "";
        }

        StringBuilder apiUrlBuilder = new StringBuilder(baseUrl);
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
     * Makes a new GET request.
     *
     * @param pathTemplate a path as {@link MessageFormat} template, like "nodes/{0}/status"
     * @param pathTemplateValues the values to fill into the template, e.g. "pve"
     * @return the new request
     */
    public Request newGetRequest(String pathTemplate, String... pathTemplateValues) {
        return newRequest(MessageFormat.format(pathTemplate, (Object[]) pathTemplateValues)).method(HttpMethod.GET);
    }

    public Request newPostRequest(String pathTemplate, String... pathTemplateValues) {
        return newRequest(MessageFormat.format(pathTemplate, (Object[]) pathTemplateValues)).method(HttpMethod.POST);
    }

    private Request newRequest(String path) {
        String validPath = path.startsWith("/") ? path : "/" + path;
        return context.getHttpClient().newRequest(apiUrl + validPath)
                .timeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).accept("application/json");
    }

    public <T> T getContent(Request request, Class<T> classToExtract)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        ContentResponse response = send(request);
        checkStatus(response);

        JsonElement data = extractData(response);
        @Nullable
        T content = context.getGson().fromJson(data, classToExtract);
        if (content == null) {
            throw new ProxmoxApiCommunicationException("No content was provided in response");
        }
        return content;
    }

    public <T> List<T> getContentAsList(Request request, Type collectionType)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        ContentResponse response = send(request);
        checkStatus(response);

        JsonElement data = extractData(response);
        List<T> content = context.getGson().fromJson(data, collectionType);
        if (content == null) {
            throw new ProxmoxApiCommunicationException("No content was provided in response");
        }
        return content;
    }

    public void sendRequest(Request request) throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        ContentResponse response = send(request);
        checkStatus(response);
    }

    private ContentResponse send(Request request) throws ProxmoxApiCommunicationException {
        try {
            return request.send();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProxmoxApiCommunicationException("Request was interrupted", ex);
        } catch (TimeoutException ex) {
            throw new ProxmoxApiCommunicationException("Request - Timeout reached", ex);
        } catch (ExecutionException ex) {
            throw new ProxmoxApiCommunicationException("Request failed", ex);
        }
    }

    private void checkStatus(ContentResponse response)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.OK_200) {
            return;
        }

        String reason = response.getReason();
        String reasonSuffix = reason.isBlank() ? "" : ": " + reason;
        if (statusCode == HttpStatus.UNAUTHORIZED_401 || statusCode == HttpStatus.FORBIDDEN_403) {
            throw new ProxmoxApiConfigurationException("Authentication failed (HTTP " + statusCode
                    + "). Please verify the user name (including the realm, e.g. openhab@pam), the password and the "
                    + "assigned permissions" + reasonSuffix);
        }
        throw new ProxmoxApiCommunicationException(
                "API call returned unexpected status code " + statusCode + reasonSuffix);
    }

    private JsonElement extractData(ContentResponse response) throws ProxmoxApiCommunicationException {
        JsonObject responseContainer = context.getGson().fromJson(response.getContentAsString(), JsonObject.class);
        if (responseContainer != null && responseContainer.has("data") && !responseContainer.get("data").isJsonNull()) {
            return responseContainer.get("data");
        }
        throw new ProxmoxApiCommunicationException("No content was provided in response");
    }
}
