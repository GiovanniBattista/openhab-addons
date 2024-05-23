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
package org.openhab.binding.nuki.internal.handler;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.nuki.internal.constants.NukiWebApiLinkBuilder;
import org.openhab.binding.nuki.internal.webapi.dataexchange.NukiWebApiHttpClient;
import org.openhab.binding.nuki.internal.webapi.dataexchange.WebApiAccountListResponse;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Zupan
 */
public class NukiWebApiBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(NukiWebApiBridgeHandler.class);
    private static final int JOB_INTERVAL = 600;

    private final HttpClient httpClient;
    @Nullable
    private NukiWebApiHttpClient nukiHttpClient;
    private NukiWebApiBridgeConfiguration config = new NukiWebApiBridgeConfiguration();

    /**
     * @param bridge
     */
    public NukiWebApiBridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        logger.debug("Instantiating NukiWebApiBridgeHandler({}, {})", bridge, httpClient);
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        this.config = getConfigAs(NukiWebApiBridgeConfiguration.class);

        String apiToken = config.apiToken;
        if (apiToken == null || apiToken.isBlank()) {
            logger.debug(
                    "NukiWebApiBridgeHandler[{}] is not initializable, apiToken setting is unset in the configuration!",
                    getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "apiToken setting is unset");
        } else {
            NukiWebApiLinkBuilder linkBuilder = new NukiWebApiLinkBuilder(); // TODO Not sue about that; but should
            // not be required as connection is
            // secured via TLS
            nukiHttpClient = new NukiWebApiHttpClient(httpClient, apiToken, linkBuilder);
            scheduler.execute(this::initializeHandler);

            // TODO: Confirmation needed: Should not be needed because web api should be (almost) always reachable
            // checkBridgeOnlineJob = scheduler.scheduleWithFixedDelay(this::checkBridgeOnline, JOB_INTERVAL,
            // JOB_INTERVAL,
            // TimeUnit.SECONDS);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub
    }

    private synchronized void initializeHandler() {
        withHttpClient(client -> {
            WebApiAccountListResponse bridgeInfoResponse = client.getAccounts();
            if (bridgeInfoResponse.getStatus() == HttpStatus.OK_200) {
                logger.debug("Web API responded with status[{}]. Switching the bridge online.",
                        bridgeInfoResponse.getStatus());
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.debug("Bridge responded with status[{}]. Switching the bridge offline!",
                        bridgeInfoResponse.getStatus());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        bridgeInfoResponse.getMessage());
            }
        });
    }

    public void withHttpClient(Consumer<NukiWebApiHttpClient> consumer) {
        withHttpClient(client -> {
            consumer.accept(client);
            return null;
        }, null);
    }

    protected <@Nullable U> @Nullable U withHttpClient(Function<NukiWebApiHttpClient, U> consumer, U defaultValue) {
        NukiWebApiHttpClient client = this.nukiHttpClient;
        if (client == null) {
            logger.warn("Nuki HTTP client is null. This is a bug in Nuki Binding, please report it",
                    new IllegalStateException());
            return defaultValue;
        } else {
            return consumer.apply(client);
        }
    }
}
