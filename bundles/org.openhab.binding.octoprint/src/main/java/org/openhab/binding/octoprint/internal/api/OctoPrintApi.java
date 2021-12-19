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
import org.openhab.binding.octoprint.internal.api.model.RegisteredSystemCommandsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Daniel Zupan - Initial contribution
 *
 */
public class OctoPrintApi {

    private final Logger logger = LoggerFactory.getLogger(OctoPrintApi.class);

    private final OctoPrintApiContext context;

    public OctoPrintApi(OctoPrintApiContext context) {
        this.context = context;
    }

    public void updateConfig(OctoPrintConfiguration config) {
        this.context.setConfig(config);
    }

    public boolean hasAppKeyWorkflowSupport() throws OctoPrintApiException {
        ContentResponse response = OctoPrintApiRequestBuilder.newBuilder(context).path("/plugin/appkeys/probe").get()
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
        ContentResponse response = OctoPrintApiRequestBuilder.newBuilder(context).path("/plugin/appkeys/request")
                .requestBody(request).post().getResponse();

        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.CREATED_201) {
            return response.getHeaders().get("Location");
        } else {
            throw new OctoPrintApiCommunicationException("App key authorization process cannot be started", null);
        }
    }

    public AuthorizationDecisionResponse getAuthorizationDecision(String generatedEndpointUrl)
            throws OctoPrintApiException {
        ContentResponse response = new OctoPrintApiRequestBuilder(context, generatedEndpointUrl).path("").get()
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

        OctoPrintApiRequestBuilder builder = OctoPrintApiRequestBuilder.newBuilder(context).path("/api/printer")
                .authenticate().get();

        int statusCode = builder.getResponse().getStatus();
        if (statusCode == HttpStatus.OK_200) {
            return builder.getContent(PrinterStateResponse.class);
        } else if (statusCode == HttpStatus.CONFLICT_409) {
            throw new OctoPrintApiCommunicationException("Printer is not operational");
        } else {
            throw new OctoPrintApiCommunicationException("Unknown error" + statusCode);
        }
    }

    /**
     * Returns all registered system commands
     *
     * @throws OctoPrintApiException
     */
    public RegisteredSystemCommandsResponse getSystemCommands() throws OctoPrintApiException {
        RegisteredSystemCommandsResponse response = OctoPrintApiRequestBuilder.newBuilder(context)
                .path("/api/system/commands").authenticate().get().getContent(RegisteredSystemCommandsResponse.class);

        return response;
    }

    /**
     * @param source
     * @param action
     * @throws OctoPrintApiException
     */
    public void executeSystemCommand(String source, String action) throws OctoPrintApiException {
        ContentResponse response = OctoPrintApiRequestBuilder.newBuilder(context)
                .path("/api/system/commands/{0}/{1}", source, action).authenticate().post().getResponse();

        logger.debug("Executing system command /api/system/commands/{0}/{1}", source, action);

        int statusCode = response.getStatus();
        if (statusCode == HttpStatus.NO_CONTENT_204) {
            logger.debug("Successfully executed system command /api/system/commands/{0}/{1}", source, action);
        } else if (statusCode == HttpStatus.BAD_REQUEST_400) {
            throw new OctoPrintApiCommunicationException(
                    "Cannot execute system command '" + action + "'. Request was malformed!");
        } else if (statusCode == HttpStatus.NOT_FOUND_404) {
            throw new OctoPrintApiCommunicationException(
                    "Command was not found for action '" + action + "' and source '" + source + "'");
        } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR_500) {
            throw new OctoPrintApiCommunicationException(
                    "An internal error occurred during execution of action '" + action + "'!");
        }
    }

    public String getOctoprintUrl() {
        return OctoPrintApiRequestBuilder.newBuilder(context).getBaseUrl();
    }

}
