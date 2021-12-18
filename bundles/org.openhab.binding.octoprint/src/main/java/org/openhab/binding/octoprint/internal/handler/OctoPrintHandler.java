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
package org.openhab.binding.octoprint.internal.handler;

import static org.openhab.binding.octoprint.internal.OctoPrintBindingConstants.*;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.octoprint.internal.OctoPrintBindingConstants;
import org.openhab.binding.octoprint.internal.OctoPrintConfiguration;
import org.openhab.binding.octoprint.internal.api.OctoPrintApi;
import org.openhab.binding.octoprint.internal.api.OctoPrintApiFactory;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiCommunicationException;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiConfigurationException;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiException;
import org.openhab.binding.octoprint.internal.api.model.ApiKeyRequest;
import org.openhab.binding.octoprint.internal.api.model.AuthorizationDecisionResponse;
import org.openhab.binding.octoprint.internal.api.model.AuthorizationDecisionResponseCode;
import org.openhab.binding.octoprint.internal.api.model.PrinterStateResponse;
import org.openhab.binding.octoprint.internal.api.model.PrinterStateTemperatureContainer;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OctoPrintHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class OctoPrintHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OctoPrintHandler.class);

    private @Nullable OctoPrintConfiguration config;

    private @Nullable ScheduledFuture<?> initializationJob;
    private @Nullable ScheduledFuture<?> authorizationDecisionPollingJobFuture;

    private final ReentrantLock pollingLock = new ReentrantLock();
    private @Nullable ScheduledFuture<?> octoPrintPollingJobFuture;

    private @Nullable OctoPrintApi api;
    private HttpClient httpClient;

    public OctoPrintHandler(Thing thing, HttpClient httpClient) {
        super(thing);

        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing the thing handler");

        config = getConfigAs(OctoPrintConfiguration.class);
        api = OctoPrintApiFactory.create(config, httpClient);

        final String hostName = config.hostname;
        if (hostName == null || hostName.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot connect to OctoPrint. Hostname or IP address is not valid or missing.");
            return;
        }

        final String user = config.user;
        if (user == null || user.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot connect to OctoPrint. User name is missing.");
            return;
        }

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        if (config.apiKey == null) {
            startApiKeyWorkflow();
        } else {
            startOctoPrintPolling();
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        stopAuthorizationDecisionPolling();
        stopOctoPrintPolling();
    }

    private void startApiKeyWorkflow() {

        scheduler.submit(() -> {
            try {
                if (!api.hasAppKeyWorkflowSupport()) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "This OctoPrint instance does not support the Application Key Workflow. Either the plugin is not installed or disabled."
                                    + "Please request the API key via the OctoPrint UI and enter it in the Thing's configuration settings!");
                    return;
                }

                String baseUrl = api.getOctoprintUrl();
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.ONLINE.CONFIGURATION_PENDING, "Please visit "
                        + baseUrl + " and accept the authorization request so that an API key can be retrieved.");

                final ApiKeyRequest request = ApiKeyRequest.of("OpenHAB", config.user);
                String pollingUrl = api.startAppKeyAuthorizationProcess(request);

                // the polling url will be considered stale and deleted internally in the OctoPrint instance if the
                // polling endpoint for it isn't called for more than 5s.
                startAuthorizationDecisionPolling(pollingUrl);

            } catch (OctoPrintApiCommunicationException ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
            } catch (OctoPrintApiConfigurationException ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
            } catch (Exception ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
            }
        });
    }

    private void startAuthorizationDecisionPolling(String pollingUrl) {
        ScheduledFuture<?> job = authorizationDecisionPollingJobFuture;
        if (job == null || job.isCancelled()) {
            AppKeyDecisionPoller poller = new AppKeyDecisionPoller(pollingUrl);
            authorizationDecisionPollingJobFuture = scheduler.scheduleWithFixedDelay(poller, 1, 1, TimeUnit.SECONDS);
        }
    }

    private void stopAuthorizationDecisionPolling() {
        ScheduledFuture<?> pollingJob = authorizationDecisionPollingJobFuture;
        if (pollingJob != null && !pollingJob.isDone()) {
            pollingJob.cancel(true);
        }
        authorizationDecisionPollingJobFuture = null;
    }

    private void startOctoPrintPolling() {
        ScheduledFuture<?> job = octoPrintPollingJobFuture;
        if (job == null || job.isCancelled()) {
            long pollingInterval = config.refreshInterval;
            if (pollingInterval < 1) {
                pollingInterval = TimeUnit.SECONDS.toSeconds(30);
                logger.warn("Wrong configuraiton value for polling interval. Using default value: {}s",
                        pollingInterval);
            }
            octoPrintPollingJobFuture = scheduler.scheduleWithFixedDelay(octoPrintPoller, 3, pollingInterval,
                    TimeUnit.SECONDS);
        }
    }

    private void stopOctoPrintPolling() {
        ScheduledFuture<?> pollingJob = octoPrintPollingJobFuture;
        if (pollingJob != null && !pollingJob.isDone()) {
            pollingJob.cancel(true);
        }
        octoPrintPollingJobFuture = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_POWER.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    abstract class AbstractPoller implements Runnable {
        @Override
        public void run() {
            try {
                pollingLock.lock();

                try {
                    performPoll();
                } catch (OctoPrintApiCommunicationException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                } catch (OctoPrintApiConfigurationException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
                } catch (Exception e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                }
            } finally {
                pollingLock.unlock();
            }
        }

        protected abstract void performPoll() throws OctoPrintApiException;
    }

    private final class AppKeyDecisionPoller extends AbstractPoller {
        private final String pollingUrl;

        private AppKeyDecisionPoller(String pollingUrl) {
            this.pollingUrl = Objects.requireNonNull(pollingUrl);
        }

        @Override
        protected void performPoll() throws OctoPrintApiException {
            AuthorizationDecisionResponse response = api.getAuthorizationDecision(pollingUrl);

            if (response.getResponseCode() == AuthorizationDecisionResponseCode.ACCESS_GRANTED) {
                authorizationDecisionPollingJobFuture.cancel(false);

                Configuration config = editConfiguration();
                config.put(OctoPrintBindingConstants.API_KEY, response.getApiKey());
                updateConfiguration(config);

                OctoPrintConfiguration octoPrintConfig = getConfigAs(OctoPrintConfiguration.class);
                updateConfig(octoPrintConfig);

                startOctoPrintPolling();
            } else if (response.getResponseCode() == AuthorizationDecisionResponseCode.ACCESS_DENIED) {
                authorizationDecisionPollingJobFuture.cancel(false);

                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                        "The access for OpenHAB was denied or a timeout occurred. Please set the API key manually by retrieving it through the OctoPrint web interface.");
            }
            // else continue polling
        }
    };

    private final Runnable octoPrintPoller = new AbstractPoller() {

        @Override
        protected void performPoll() throws OctoPrintApiException {
            PrinterStateResponse printerState = api.getPrinterState();

            if (thing.getStatus() != ThingStatus.ONLINE
                    || thing.getStatusInfo().getStatusDetail() != ThingStatusDetail.NONE) {
                updateStatus(ThingStatus.ONLINE);
            }

            PrinterStateTemperatureContainer temperature = printerState.getTemperature();
            updateState(CHANNEL_CURRENT_TEMPERATURE_TOOL0, new DecimalType(temperature.getTool0().getActual()));
            updateState(CHANNEL_TARGET_TEMPERATURE_TOOL0, new DecimalType(temperature.getTool0().getTarget()));

            updateState(CHANNEL_CURRENT_TEMPERATURE_BED, new DecimalType(temperature.getBed().getActual()));
            updateState(CHANNEL_TARGET_TEMPERATURE_BED, new DecimalType(temperature.getBed().getTarget()));
        }
    };

    /**
     * @param config the config to set
     */
    public void updateConfig(OctoPrintConfiguration config) {
        this.config = config;
        this.api.updateConfig(config);
    }

}
