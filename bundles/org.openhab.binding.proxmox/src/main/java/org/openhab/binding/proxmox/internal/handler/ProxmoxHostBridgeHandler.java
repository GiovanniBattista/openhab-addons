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
package org.openhab.binding.proxmox.internal.handler;

import static org.openhab.binding.proxmox.internal.ProxmoxBindingConstants.*;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApi;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApiFactory;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxLxc;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxNode;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVersion;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVm;
import org.openhab.binding.proxmox.internal.config.ProxmoxHostConfiguration;
import org.openhab.binding.proxmox.internal.discovery.ProxmoxDiscoveryService;
import org.openhab.binding.proxmox.internal.utils.WakeOnLanUtility;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ProxmoxHostBridgeHandler} connects to a Proxmox VE host, polls it for the state of all
 * nodes, VMs and containers and fans the results out to the registered child thing handlers.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxHostBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(ProxmoxHostBridgeHandler.class);

    private static final int WOL_PACKET_RETRY_COUNT = 10;
    private static final int WOL_PACKET_RETRY_DELAY_MILLIS = 100;
    private static final int DEFAULT_POLLING_INTERVAL = 30;

    private volatile ProxmoxHostConfiguration config = new ProxmoxHostConfiguration();
    private volatile @Nullable ProxmoxVEApi api;
    private final HttpClientFactory httpClientFactory;
    // Only set when this handler owns a private (trust-all) client that it has to stop again on dispose.
    private @Nullable HttpClient ownHttpClient;

    private final Map<String, ProxmoxNode> lastNodeStates = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxVm> lastVmStates = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxLxc> lastLxcStates = new ConcurrentHashMap<>();

    private @Nullable ProxmoxDiscoveryService discoveryService;
    private final Map<String, ProxmoxStatusChangedListener<ProxmoxNode>> nodeStatusListeners = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxStatusChangedListener<ProxmoxVm>> vmStatusListeners = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxStatusChangedListener<ProxmoxLxc>> lxcStatusListeners = new ConcurrentHashMap<>();

    private final ReentrantLock pollingLock = new ReentrantLock();
    private @Nullable ScheduledFuture<?> proxmoxPollingJob;
    private volatile boolean macAddressDetectionDone = false;

    public ProxmoxHostBridgeHandler(Bridge bridge, HttpClientFactory httpClientFactory) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing host bridge handler");

        ProxmoxHostConfiguration config = getConfigAs(ProxmoxHostConfiguration.class);
        this.config = config;

        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No base URL set");
            return;
        }

        try {
            new URI(baseUrl).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Invalid base URL: " + ex.getMessage());
            return;
        }

        if (!config.usesApiToken()) {
            String username = config.getUsername();
            String password = config.getPassword();
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Provide either an API token (id and secret) or a user name and password");
                return;
            }
        }

        HttpClient httpClient;
        try {
            httpClient = createHttpClient(config);
        } catch (ProxmoxApiConfigurationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
            return;
        }
        this.api = ProxmoxVEApiFactory.create(config, httpClient);

        // Set the thing status to UNKNOWN temporarily and let the background polling task decide the real status.
        updateStatus(ThingStatus.UNKNOWN);

        startProxmoxPolling();
    }

    /**
     * Returns the {@link HttpClient} to use for this bridge. When TLS validation is disabled a private trust-all client
     * is created (and later stopped on {@link #dispose()}); otherwise the shared common client is reused.
     */
    private HttpClient createHttpClient(ProxmoxHostConfiguration config) throws ProxmoxApiConfigurationException {
        stopOwnHttpClient();
        if (config.isTrustAllCertificates()) {
            HttpClient client = new HttpClient(new SslContextFactory.Client(true));
            try {
                client.start();
            } catch (Exception ex) {
                throw new ProxmoxApiConfigurationException("Could not start HTTP client: " + ex.getMessage(), ex);
            }
            ownHttpClient = client;
            return client;
        }
        return httpClientFactory.getCommonHttpClient();
    }

    private void stopOwnHttpClient() {
        HttpClient client = ownHttpClient;
        ownHttpClient = null;
        if (client != null) {
            try {
                client.stop();
            } catch (Exception ex) {
                logger.debug("Failed to stop HTTP client: {}", ex.getMessage());
            }
        }
    }

    private void startProxmoxPolling() {
        ScheduledFuture<?> job = proxmoxPollingJob;
        if (job == null || job.isCancelled()) {
            int pollingInterval = config.getPollingInterval();
            if (pollingInterval < 1) {
                pollingInterval = DEFAULT_POLLING_INTERVAL;
                logger.warn("Wrong configuration value for polling interval. Using default value: {}s",
                        pollingInterval);
            }
            proxmoxPollingJob = scheduler.scheduleWithFixedDelay(proxmoxApiPoller, 3, pollingInterval,
                    TimeUnit.SECONDS);
        }
    }

    private void stopProxmoxPolling() {
        ScheduledFuture<?> pollingJob = proxmoxPollingJob;
        if (pollingJob != null && !pollingJob.isDone()) {
            pollingJob.cancel(true);
        }
        proxmoxPollingJob = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Bridge does not support commands. Ignoring command '{}' for channel '{}'.", command, channelUID);
    }

    @Override
    public void dispose() {
        super.dispose();
        stopProxmoxPolling();
        stopOwnHttpClient();
    }

    public @Nullable ProxmoxVEApi getApi() {
        return api;
    }

    abstract class AbstractPoller implements Runnable {
        @Override
        public void run() {
            pollingLock.lock();
            try {
                fetchStatusUpdates();

                if (thing.getStatus() != ThingStatus.ONLINE) {
                    updateVersionProperty();
                    updateStatus(ThingStatus.ONLINE);
                    scheduleMacAddressDetection();
                }
            } catch (ProxmoxApiCommunicationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (ProxmoxApiConfigurationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            } catch (RuntimeException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } finally {
                pollingLock.unlock();
            }
        }

        protected abstract void fetchStatusUpdates()
                throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException;
    }

    private final Runnable proxmoxApiPoller = new AbstractPoller() {

        @Override
        protected void fetchStatusUpdates() throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
            ProxmoxVEApi api = getApi();
            if (api == null) {
                throw new ProxmoxApiCommunicationException("API is not initialized");
            }

            Map<String, ProxmoxNode> lastNodeStatesCopy = new HashMap<>(lastNodeStates);
            Map<String, ProxmoxVm> lastVmStatesCopy = new HashMap<>(lastVmStates);
            Map<String, ProxmoxLxc> lastLxcStatesCopy = new HashMap<>(lastLxcStates);

            final ProxmoxDiscoveryService discovery = discoveryService;
            for (ProxmoxNode node : api.getNodes()) {
                String id = node.getNode();
                if (id == null) {
                    continue;
                }

                ProxmoxStatusChangedListener<ProxmoxNode> nodeStatusListener = nodeStatusListeners.get(id);
                if (nodeStatusListener == null) {
                    logger.trace("Node '{}' was added", id);

                    if (discovery != null && !lastNodeStatesCopy.containsKey(id)) {
                        discovery.notifyNodeDiscovered(node);
                    }

                    lastNodeStates.put(id, node);
                } else {
                    if (nodeStatusListener.onStateChanged(node)) {
                        lastNodeStates.put(id, node);
                    }
                }
                // node was handled, so remove it from the copy (if this node exists)
                lastNodeStatesCopy.remove(id);

                fetchStatusUpdates4VMs(api, node, lastVmStatesCopy);
                fetchStatusUpdates4LXCs(api, node, lastLxcStatesCopy);
            }

            // the remaining nodes in lastNodeStatesCopy were not handled, thus have to be removed
            lastNodeStatesCopy.forEach((id, node) -> {
                logger.trace("Node '{}' removed.", id);
                lastNodeStates.remove(id);

                ProxmoxStatusChangedListener<ProxmoxNode> statusListener = nodeStatusListeners.get(id);
                if (statusListener != null) {
                    statusListener.onRemoved();
                }

                if (discovery != null) {
                    discovery.removeDiscoveredNode(node);
                }
            });

            // the remaining VMs in lastVmStatesCopy were not handled, thus have to be removed
            lastVmStatesCopy.forEach((id, vm) -> {
                logger.trace("VM '{}' removed.", id);
                lastVmStates.remove(id);

                ProxmoxStatusChangedListener<ProxmoxVm> statusListener = vmStatusListeners.get(id);
                if (statusListener != null) {
                    statusListener.onRemoved();
                }

                if (discovery != null) {
                    discovery.removeDiscoveredVM(vm);
                }
            });

            // the remaining LXCs in lastLxcStatesCopy were not handled, thus have to be removed
            lastLxcStatesCopy.forEach((id, lxc) -> {
                logger.trace("LXC '{}' removed.", id);
                lastLxcStates.remove(id);

                ProxmoxStatusChangedListener<ProxmoxLxc> statusListener = lxcStatusListeners.get(id);
                if (statusListener != null) {
                    statusListener.onRemoved();
                }

                if (discovery != null) {
                    discovery.removeDiscoveredLxc(lxc);
                }
            });
        }

        private void fetchStatusUpdates4VMs(ProxmoxVEApi api, ProxmoxNode node, Map<String, ProxmoxVm> lastVMStatesCopy)
                throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

            final ProxmoxDiscoveryService discovery = discoveryService;
            for (ProxmoxVm vm : api.getVMs(node)) {
                String id = vm.getVmid();
                if (id == null) {
                    continue;
                }

                ProxmoxStatusChangedListener<ProxmoxVm> vmStatusListener = vmStatusListeners.get(id);
                if (vmStatusListener == null) {
                    logger.trace("VM '{}' was added", id);

                    if (discovery != null && !lastVMStatesCopy.containsKey(id)) {
                        discovery.notifyVmDiscovered(vm, node);
                    }

                    lastVmStates.put(id, vm);
                } else {
                    if (vmStatusListener.onStateChanged(vm)) {
                        lastVmStates.put(id, vm);
                    }
                }
                // VM was handled, so remove it from the copy (if this VM exists)
                lastVMStatesCopy.remove(id);
            }
        }

        private void fetchStatusUpdates4LXCs(ProxmoxVEApi api, ProxmoxNode node,
                Map<String, ProxmoxLxc> lastLxcStatesCopy)
                throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

            final ProxmoxDiscoveryService discovery = discoveryService;
            for (ProxmoxLxc lxc : api.getLXCs(node)) {
                String id = lxc.getLxcId();
                if (id == null) {
                    continue;
                }

                ProxmoxStatusChangedListener<ProxmoxLxc> lxcStatusListener = lxcStatusListeners.get(id);
                if (lxcStatusListener == null) {
                    logger.trace("LXC '{}' was added", id);

                    if (discovery != null && !lastLxcStatesCopy.containsKey(id)) {
                        discovery.notifyLxcDiscovered(lxc, node);
                    }

                    lastLxcStates.put(id, lxc);
                } else {
                    if (lxcStatusListener.onStateChanged(lxc)) {
                        lastLxcStates.put(id, lxc);
                    }
                }
                // LXC was handled, so remove it from the copy (if this LXC exists)
                lastLxcStatesCopy.remove(id);
            }
        }
    };

    public @Nullable ProxmoxNode getNodeById(String nodeId) {
        return lastNodeStates.get(nodeId);
    }

    private void updateVersionProperty() {
        ProxmoxVEApi localApi = api;
        if (localApi == null) {
            return;
        }
        try {
            ProxmoxVersion version = localApi.getVersion();
            String versionString = version.getVersion();
            if (versionString != null) {
                updateProperty(PROPERTY_HOST_VERSION, versionString);
            }
        } catch (ProxmoxApiCommunicationException | ProxmoxApiConfigurationException e) {
            logger.debug("Unable to determine Proxmox VE version: {}", e.getMessage());
        }
    }

    /**
     * Triggers a one-time, asynchronous auto-detection of the host MAC address. Detection uses a blocking {@code arp}
     * call, so it must not run on the polling thread. It is skipped entirely when the user has configured a MAC address
     * manually and runs at most once per handler lifecycle.
     */
    private void scheduleMacAddressDetection() {
        if (macAddressDetectionDone) {
            return;
        }
        macAddressDetectionDone = true;

        String configured = config.getMacAddress();
        if (configured != null && !configured.isEmpty()) {
            return;
        }
        scheduler.execute(this::detectAndStoreMacAddress);
    }

    private void detectAndStoreMacAddress() {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            return;
        }

        try {
            String host = new URI(baseUrl).getHost();
            if (host == null) {
                return;
            }
            String macAddress = WakeOnLanUtility.getMACAddress(InetAddress.getByName(host).getHostAddress());
            if (macAddress != null && !macAddress.equals(config.getMacAddress())) {
                Configuration configuration = editConfiguration();
                configuration.put(CONFIG_MAC_ADDRESS, macAddress);
                updateConfiguration(configuration);
            }
        } catch (URISyntaxException | UnknownHostException e) {
            logger.debug("Unable to determine MAC address: {}", e.getMessage());
        }
    }

    public @Nullable ProxmoxVm getVmById(String vmId) {
        return lastVmStates.get(vmId);
    }

    public @Nullable ProxmoxLxc getLxcById(String lxcId) {
        return lastLxcStates.get(lxcId);
    }

    public void registerNodeStatusChangeListener(String nodeId,
            ProxmoxStatusChangedListener<ProxmoxNode> statusChangeListener) {
        if (!nodeStatusListeners.containsKey(nodeId)) {
            nodeStatusListeners.put(nodeId, statusChangeListener);
            ProxmoxNode node = lastNodeStates.get(nodeId);
            if (node != null) {
                statusChangeListener.onAdded(node);
            }
        }
    }

    public void unregisterNodeStatusChangeListener(String nodeId) {
        nodeStatusListeners.remove(nodeId);
    }

    public void registerVmStatusChangeListener(String vmId,
            ProxmoxStatusChangedListener<ProxmoxVm> statusChangeListener) {
        if (!vmStatusListeners.containsKey(vmId)) {
            vmStatusListeners.put(vmId, statusChangeListener);
            ProxmoxVm vm = lastVmStates.get(vmId);
            if (vm != null) {
                statusChangeListener.onAdded(vm);
            }
        }
    }

    public void unregisterVmStatusChangeListener(String vmId) {
        vmStatusListeners.remove(vmId);
    }

    public void registerLxcStatusChangeListener(String lxcId,
            ProxmoxStatusChangedListener<ProxmoxLxc> statusChangeListener) {
        if (!lxcStatusListeners.containsKey(lxcId)) {
            lxcStatusListeners.put(lxcId, statusChangeListener);
            ProxmoxLxc lxc = lastLxcStates.get(lxcId);
            if (lxc != null) {
                statusChangeListener.onAdded(lxc);
            }
        }
    }

    public void unregisterLxcStatusChangeListener(String lxcId) {
        lxcStatusListeners.remove(lxcId);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(ProxmoxDiscoveryService.class);
    }

    public void registerDiscoveryListener(ProxmoxDiscoveryService proxmoxDiscoveryService) {
        if (discoveryService == null) {
            discoveryService = proxmoxDiscoveryService;
        }
    }

    public void wakeOnLan(String nodeName) {
        String macAddress = config.getMacAddress();
        if (macAddress == null || macAddress.isEmpty()) {
            logger.debug("ON command for Node '{}' was triggered. Cannot use the Proxmox API because the host is down. "
                    + "Trying to wake up the host which should also power the node, but the MAC address needs to be set "
                    + "in the thing configuration for this to work.", nodeName);
        } else {
            String mac = macAddress;
            for (int i = 0; i < WOL_PACKET_RETRY_COUNT; i++) {
                scheduler.schedule(() -> {
                    try {
                        WakeOnLanUtility.sendWOLPacket(mac);
                    } catch (IllegalArgumentException ex) {
                        logger.debug("Failed to send WOL packet: {}", ex.getMessage());
                    }
                }, (long) i * WOL_PACKET_RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS);
            }
        }
    }
}
