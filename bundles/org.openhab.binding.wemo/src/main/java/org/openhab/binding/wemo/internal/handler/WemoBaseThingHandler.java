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
package org.openhab.binding.wemo.internal.handler;

import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.openhab.binding.wemo.internal.WemoBindingConstants;
import org.openhab.binding.wemo.internal.http.WemoHttpCall;
import org.openhab.core.io.transport.upnp.UpnpIOParticipant;
import org.openhab.core.io.transport.upnp.UpnpIOService;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WemoBaseThingHandler} provides a base implementation for the
 * concrete WeMo handlers.
 *
 * @author Jacob Laursen - Initial contribution
 */
@NonNullByDefault
public abstract class WemoBaseThingHandler extends BaseThingHandler implements UpnpIOParticipant {

    private static final int SUBSCRIPTION_RENEWAL_INITIAL_DELAY_SECONDS = 15;
    private static final int SUBSCRIPTION_RENEWAL_INTERVAL_SECONDS = 60;

    private final Logger logger = LoggerFactory.getLogger(WemoBaseThingHandler.class);
    private final UpnpIOService service;
    private final UpnpService upnpService;

    protected WemoHttpCall wemoHttpCaller;

    private @Nullable String host;
    private Map<String, Instant> subscriptions = new ConcurrentHashMap<String, Instant>();
    private @Nullable ScheduledFuture<?> subscriptionRenewalJob;

    public WemoBaseThingHandler(Thing thing, UpnpIOService upnpIOService, UpnpService upnpService,
            WemoHttpCall wemoHttpCaller) {
        super(thing);
        this.service = upnpIOService;
        this.upnpService = upnpService;
        this.wemoHttpCaller = wemoHttpCaller;
    }

    @Override
    public void initialize() {
        logger.debug("Registering UPnP participant for {}", getThing().getUID());
        service.registerParticipant(this);
        initializeHost();
    }

    @Override
    public void dispose() {
        removeSubscriptions();
        logger.debug("Unregistering UPnP participant for {}", getThing().getUID());
        cancelSubscriptionRenewalJob();
        service.unregisterParticipant(this);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // can be overridden by subclasses
    }

    @Override
    public void onStatusChanged(boolean status) {
        if (status) {
            logger.debug("UPnP device {} for {} is present", getUDN(), getThing().getUID());
            if (service.isRegistered(this)) {
                // After successful discovery, try to subscribe again.
                renewSubscriptions();
            }
        } else {
            logger.info("UPnP device {} for {} is absent", getUDN(), getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
            // Expire subscriptions.
            for (Entry<String, Instant> subscription : subscriptions.entrySet()) {
                subscription.setValue(Instant.MIN);
            }
        }
    }

    @Override
    public void onValueReceived(@Nullable String variable, @Nullable String value, @Nullable String service) {
        // can be overridden by subclasses
    }

    @Override
    public void onServiceSubscribed(@Nullable String service, boolean succeeded) {
        if (service == null) {
            return;
        }
        logger.debug("Subscription to service {} for {} {}", service, getUDN(), succeeded ? "succeeded" : "failed");
        if (succeeded) {
            subscriptions.put(service, Instant.now());
        }
    }

    @Override
    public @Nullable String getUDN() {
        return (String) this.getConfig().get(WemoBindingConstants.UDN);
    }

    protected boolean isUpnpDeviceRegistered() {
        return service.isRegistered(this);
    }

    protected void addSubscription(String serviceId) {
        if (subscriptions.containsKey(serviceId)) {
            logger.debug("{} already subscribed to {}", getUDN(), serviceId);
            return;
        }
        if (subscriptions.isEmpty()) {
            logger.debug("Adding first GENA subscription for {}, scheduling renewal job", getUDN());
            scheduleSubscriptionRenewalJob();
        }
        subscriptions.put(serviceId, Instant.MIN);
        logger.debug("Adding GENA subscription {} for {}, participant is {}", serviceId, getUDN(),
                service.isRegistered(this) ? "registered" : "not registered");
        service.addSubscription(this, serviceId, WemoBindingConstants.SUBSCRIPTION_DURATION_SECONDS);
    }

    private void scheduleSubscriptionRenewalJob() {
        cancelSubscriptionRenewalJob();
        this.subscriptionRenewalJob = scheduler.scheduleWithFixedDelay(this::renewSubscriptions,
                SUBSCRIPTION_RENEWAL_INITIAL_DELAY_SECONDS, SUBSCRIPTION_RENEWAL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelSubscriptionRenewalJob() {
        ScheduledFuture<?> subscriptionRenewalJob = this.subscriptionRenewalJob;
        if (subscriptionRenewalJob != null) {
            subscriptionRenewalJob.cancel(true);
        }
        this.subscriptionRenewalJob = null;
    }

    private synchronized void renewSubscriptions() {
        if (subscriptions.isEmpty()) {
            return;
        }
        if (!service.isRegistered(this)) {
            logger.debug("Participant not registered when renewing GENA subscriptions for {}, starting UPnP discovery",
                    getUDN());
            upnpService.getControlPoint().search(new RootDeviceHeader());
            return;
        }
        logger.debug("Renewing GENA subscriptions for {}", getUDN());
        subscriptions.forEach((serviceId, lastRenewed) -> {
            if (lastRenewed.isBefore(Instant.now().minusSeconds(
                    WemoBindingConstants.SUBSCRIPTION_DURATION_SECONDS - SUBSCRIPTION_RENEWAL_INTERVAL_SECONDS))) {
                logger.debug("Subscription for service {} with timestamp {} has expired, renewing", serviceId,
                        lastRenewed);
                service.removeSubscription(this, serviceId);
                service.addSubscription(this, serviceId, WemoBindingConstants.SUBSCRIPTION_DURATION_SECONDS);
            }
        });
    }

    private void removeSubscriptions() {
        if (subscriptions.isEmpty()) {
            return;
        }
        logger.debug("Removing GENA subscriptions for {}, participant is {}", getUDN(),
                service.isRegistered(this) ? "registered" : "not registered");
        subscriptions.forEach((serviceId, lastRenewed) -> {
            logger.debug("Removing subscription for service {}", serviceId);
            service.removeSubscription(this, serviceId);
        });
        subscriptions.clear();
    }

    public @Nullable String getWemoURL(String actionService) {
        String host = getHost();
        if (host == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/config-status.error.missing-ip");
            return null;
        }
        int port = scanForPort(host);
        if (port == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/config-status.error.missing-url");
            return null;
        }
        return "http://" + host + ":" + port + "/upnp/control/" + actionService + "1";
    }

    private @Nullable String getHost() {
        if (host != null) {
            return host;
        }
        initializeHost();
        return host;
    }

    private void initializeHost() {
        host = getHostFromService();
    }

    private int scanForPort(String host) {
        int portCheckStart = 49151;
        int portCheckStop = 49157;
        int port = 0;
        for (int portCheck = portCheckStart; portCheck < portCheckStop; portCheck++) {
            String urlProbe = "http://" + host + ":" + portCheck;
            logger.trace("Probing {} to find port", urlProbe);
            if (!wemoHttpCaller.probeURL(urlProbe)) {
                continue;
            }
            port = portCheck;
            logger.trace("Successfully detected port {}", port);
            break;
        }
        return port;
    }

    private @Nullable String getHostFromService() {
        URL descriptorURL = service.getDescriptorURL(this);
        if (descriptorURL != null) {
            return descriptorURL.getHost();
        }
        return null;
    }
}
