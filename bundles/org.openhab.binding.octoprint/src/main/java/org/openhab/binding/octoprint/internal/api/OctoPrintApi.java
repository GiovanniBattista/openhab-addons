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

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.octoprint.internal.OctoPrintConfiguration;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiCommunicationException;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiException;
import org.openhab.binding.octoprint.internal.api.model.ApiKeyRequest;
import org.openhab.binding.octoprint.internal.api.model.AuthorizationDecisionResponse;
import org.openhab.binding.octoprint.internal.api.model.AuthorizationDecisionResponseCode;
import org.openhab.binding.octoprint.internal.api.model.PrinterStateResponse;

import com.google.gson.JsonObject;

/**
 * @author Daniel Zupan - Initial contribution
 *
 */
public class OctoPrintApi {

    private final OctoPrintApiContext context;

    public OctoPrintApi(OctoPrintApiContext context) {
        this.context = context;
    }

    public void updateConfig(OctoPrintConfiguration config) {
        this.context.setConfig(config);
    }

    public boolean hasAppKeyWorkflowSupport() throws OctoPrintApiException {
        ContentResponse response = OctoPrintApiRequestBuilder.newBuilder(context).get("/plugin/appkeys/probe").send()
                .getResponse();

        int statusCode = response.getStatus();
        return statusCode == HttpStatus.NO_CONTENT_204;
    }

    /**
     * Starts the app key authorization process. Returns the polling URL.
     *
     * @param request
     * @return
     * @throws OctoPrintApiException
     */
    public String startAppKeyAuthorizationProcess(ApiKeyRequest request) throws OctoPrintApiException {
        ContentResponse response = OctoPrintApiRequestBuilder.newBuilder(context).post("/plugin/appkeys/request")
                .requestBody(request).send().getResponse();

        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.CREATED_201) {
            return response.getHeaders().get("Location");
        } else {
            throw new OctoPrintApiCommunicationException("App key authorization process cannot be started", null);
        }
    }

    public AuthorizationDecisionResponse getAuthorizationDecision(String generatedEndpointUrl)
            throws OctoPrintApiException {
        ContentResponse response = new OctoPrintApiRequestBuilder(context, generatedEndpointUrl).get().send()
                .getResponse();

        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.OK_200) {
            JsonObject apiKeyResponse = context.getGson().fromJson(response.getContentAsString(), JsonObject.class);
            String apiKey = apiKeyResponse.get("api_key").getAsString();
            return new AuthorizationDecisionResponse(AuthorizationDecisionResponseCode.ACCESS_GRANTED, apiKey);
        } else if (statusCode == HttpStatus.ACCEPTED_202) {
            return new AuthorizationDecisionResponse(AuthorizationDecisionResponseCode.CONTINUE_POLLING);
        } else {
            return new AuthorizationDecisionResponse(AuthorizationDecisionResponseCode.ACCESS_DENIED);
        }
    }

    public PrinterStateResponse getPrinterState() throws OctoPrintApiException {

        OctoPrintApiRequestBuilder builder = OctoPrintApiRequestBuilder.newBuilder(context).get("/api/printer")
                .authenticate().send();

        int statusCode = builder.getResponse().getStatus();
        if (statusCode == HttpStatus.OK_200) {
            return builder.getContent(PrinterStateResponse.class);
        } else if (statusCode == HttpStatus.CONFLICT_409) {
            throw new OctoPrintApiCommunicationException("Printer is not operational");
        } else {
            throw new OctoPrintApiCommunicationException("Unknown error" + statusCode);
        }
    }

    public String getOctoprintUrl() {
        return OctoPrintApiRequestBuilder.newBuilder(context).getBaseUrl();
    }

}
