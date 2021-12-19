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
package org.openhab.binding.octoprint.internal.api;

import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiCommunicationException;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiException;

/**
 * @author Daniel Zupan - Initial contribution
 *
 */
public class OctoPrintApiRequestBuilder {
    private static final String UNSECURED_BASE_URL = "http://{0}:{1,number,#}{2}";

    private final OctoPrintApiContext context;
    private final String apiUrl;

    private Request request;
    private ContentResponse response;

    public static OctoPrintApiRequestBuilder newBuilder(OctoPrintApiContext context) {
        String apiUrl = MessageFormat.format(UNSECURED_BASE_URL,
                new Object[] { context.getConfig().hostname, context.getConfig().port, context.getConfig().path });
        return new OctoPrintApiRequestBuilder(context, apiUrl);
    }

    public OctoPrintApiRequestBuilder(OctoPrintApiContext context, String apiUrl) {
        this.context = context;
        this.apiUrl = apiUrl;
    }

    public OctoPrintApiRequestBuilder path(String pathTemplate, String... pathTemplateValues) {
        request = newRequest(MessageFormat.format(pathTemplate, (Object[]) pathTemplateValues));
        return this;
    }

    private Request newRequest(String path) {
        // TODO Make timeout configurable?
        String fullUrl = MessageFormat.format("{0}{1}", apiUrl, path);

        return context.getHttpClient().newRequest(fullUrl).timeout(5000, TimeUnit.MILLISECONDS)
                .accept("application/json");
    }

    public OctoPrintApiRequestBuilder get() throws OctoPrintApiException {

        if (request == null) {
            throw new IllegalStateException("Request was not properly build. Call .path first!");
        }

        request.method(HttpMethod.GET);
        return send();
    }

    public OctoPrintApiRequestBuilder post() throws OctoPrintApiException {
        if (request == null) {
            throw new IllegalStateException("Request was not properly build. Call .path first!");
        }

        request.method(HttpMethod.POST).header("Content-Type", "application/json");
        return send();
    }

    public OctoPrintApiRequestBuilder requestBody(Object body) {
        if (request == null) {
            throw new IllegalStateException("Request was not properly build. Call .path first!");
        }

        String json = context.getGson().toJson(body);
        request.content(new StringContentProvider(json));

        return this;
    }

    public OctoPrintApiRequestBuilder authenticate() {

        if (request == null) {
            throw new IllegalStateException("Request was not properly build. Call .path first!");
        }

        if (context.getConfig() == null || context.getConfig().apiKey == null) {
            throw new IllegalStateException("Api key was not set but is required for authentication!");
        }

        String authorizationHeader = "Bearer " + context.getConfig().apiKey;
        request.header("Authorization", authorizationHeader);

        return this;
    }

    private OctoPrintApiRequestBuilder send() throws OctoPrintApiException {

        if (request == null) {
            throw new IllegalStateException("Request was not properly build. Call .path first!");
        }

        try {
            response = request.send();
        } catch (InterruptedException e) {
            throw new OctoPrintApiCommunicationException("Request was interrupted: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new OctoPrintApiCommunicationException("Request - Timeout reached: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new OctoPrintApiCommunicationException("Request failed: " + e.getMessage(), e);
        }

        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.FORBIDDEN_403) {
            throw new OctoPrintApiCommunicationException(
                    "apiKey configuration property is invalid! Please generate another one or empty it to retrieve a proper one automatically.");
        }

        return this;
    }

    public <T> T getContent(Class<T> contentClass) throws OctoPrintApiException {

        if (response == null) {
            throw new IllegalStateException("No response yet! Have you called .send()?");
        }

        int statusCode = getResponse().getStatus();
        if (statusCode != HttpStatus.OK_200) {
            throw new OctoPrintApiCommunicationException("Unexpected response: " + statusCode);
        }

        return context.getGson().fromJson(getResponse().getContentAsString(), contentClass);
    }

    /**
     * @return the response
     */
    public ContentResponse getResponse() {
        return response;
    }

    /**
     * @return the unsecuredBaseUrl
     */
    public String getBaseUrl() {
        return apiUrl;
    }

}
