package org.openhab.binding.proxmox.internal.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApi;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApiFactory;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxLxc;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxNode;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVm;
import org.openhab.binding.proxmox.internal.config.ProxmoxHostConfiguration;
import org.openhab.binding.proxmox.internal.discovery.ProxmoxDiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxmoxHostBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(ProxmoxHostBridgeHandler.class);

    private ProxmoxHostConfiguration config;
    private ProxmoxVEApi api;
    private HttpClient httpClient;

    private final Map<String, ProxmoxNode> lastNodeStates = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxVm> lastVmStates = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxLxc> lastLxcStates = new ConcurrentHashMap<>();

    private @Nullable ProxmoxDiscoveryService discoveryService;
    private final Map<String, ProxmoxStatusChangedListener<ProxmoxNode>> nodeStatusListeners = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxStatusChangedListener<ProxmoxVm>> vmStatusListeners = new ConcurrentHashMap<>();
    private final Map<String, ProxmoxStatusChangedListener<ProxmoxLxc>> lxcStatusListeners = new ConcurrentHashMap<>();

    private boolean bridgeConnectedToHost = false;
    final ReentrantLock pollingLock = new ReentrantLock();
    private @Nullable ScheduledFuture<?> proxmoxPollingJob;

    public ProxmoxHostBridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);

        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing host bridge handler");

        config = getConfigAs(ProxmoxHostConfiguration.class);
        api = ProxmoxVEApiFactory.create(config, httpClient);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

        if (config.getBaseUrl() == null || config.getBaseUrl().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No base url set");
            return;
        }

        if (config.getUsername() == null || config.getUsername().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No username set");
            return;
        }

        if (config.getPassword() == null || config.getPassword().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No password set");
            return;
        }

        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        startProxmoxPolling();

        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");
    }

    // private void initializeBridgeStatusAndPropertiesIfOffline() {
    // Bridge bridge = getBridge();
    // if (bridge != null && bridge.getStatus() == ThingStatus.ONLINE) {
    // return;
    // }
    //
    // // try to get the version of the Proxmox API which requires an authentication
    // try {
    // ProxmoxVersion version = api.getVersion();
    // logger.debug("Proxmox API version: {}", version.getVersion());
    // updateStatus(ThingStatus.ONLINE);
    //
    // startProxmoxPolling();
    // } catch (ProxmoxApiCommunicationException | ProxmoxApiInvalidResponseException e) {
    // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    // } catch (ProxmoxApiConfigurationException e) {
    // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
    // }
    // return;
    // }

    private void startProxmoxPolling() {
        ScheduledFuture<?> job = proxmoxPollingJob;
        if (job == null || job.isCancelled()) {
            long pollingInterval = config.getPollingInterval();
            if (pollingInterval < 1) {
                pollingInterval = TimeUnit.SECONDS.toSeconds(30);
                logger.warn("Wrong configuraiton value for polling interval. Using default value: {}s",
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
            pollingJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (CHANNEL_1.equals(channelUID.getId())) {
        // if (command instanceof RefreshType) {
        // // TODO: handle data refresh
        // }
        //
        // // TODO: handle command
        //
        // // Note: if communication with thing fails for some reason,
        // // indicate that by setting the status with detail information:
        // // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // // "Could not control device at IP address x.x.x.x");
        // }
    }

    @Override
    public void dispose() {
        super.dispose();

        stopProxmoxPolling();

        // if (this.initializationFuture != null && !this.initializationFuture.isDone()) {
        // this.initializationFuture.cancel(true);
        // this.initializationFuture = null;
        // }
    }

    public ProxmoxVEApi getApi() {
        return api;
    }

    abstract class AbstractPoller implements Runnable {
        @Override
        public void run() {
            try {
                pollingLock.lock();
                if (!bridgeConnectedToHost) {
                    bridgeConnectedToHost = tryResumeHostConnection();
                }

                if (bridgeConnectedToHost) {
                    try {
                        fetchStatusUpdates();

                        if (thing.getStatus() != ThingStatus.ONLINE) {
                            updateStatus(ThingStatus.ONLINE);
                        }
                    } catch (ProxmoxApiCommunicationException e) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    } catch (ProxmoxApiConfigurationException e) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
                    }
                }
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
            Map<String, ProxmoxNode> lastNodeStatesCopy = new HashMap<>(lastNodeStates);
            Map<String, ProxmoxVm> lastVmStatesCopy = new HashMap<>(lastVmStates);
            Map<String, ProxmoxLxc> lastLxcStatesCopy = new HashMap<>(lastLxcStates);

            // TODO nodes/vm/lxc should maybe not auto discovered?
            final ProxmoxDiscoveryService discovery = discoveryService;
            for (ProxmoxNode node : getApi().getNodes()) {
                String id = node.getNode();

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

                fetchstatusUpdates4VMs(node, lastVmStatesCopy);
                fetchStatusUpdates4LXCs(node, lastLxcStatesCopy);
            }

            // the remaining nodes in lastNodeStatesCopy were not handled, thus have to be removed
            lastNodeStatesCopy.forEach((id, node) -> {
                logger.trace("Node '{}' removed.", id);
                lastNodeStates.remove(id);

                ProxmoxStatusChangedListener<ProxmoxNode> statusListener = nodeStatusListeners.get(id);
                if (statusListener != null) {
                    statusListener.onRemoved();
                }

                if (discovery != null && node != null) {
                    discovery.removeDiscoveredNode(node);
                }
            });
        }

        private void fetchstatusUpdates4VMs(ProxmoxNode node, Map<String, ProxmoxVm> lastVMStatesCopy)
                throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

            final ProxmoxDiscoveryService discovery = discoveryService;
            for (ProxmoxVm vm : getApi().getVMs(node)) {
                String id = vm.getVmid();

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
                // VM was handled, so remove it from the copy (if this node exists)
                lastVMStatesCopy.remove(id);
            }

            // the remaining VMs in lastVMStatesCopy were not handled, thus have to be removed
            lastVMStatesCopy.forEach((id, vm) -> {
                logger.trace("VM '{}' removed.", id);
                lastVmStates.remove(id);

                ProxmoxStatusChangedListener<ProxmoxVm> statusListener = vmStatusListeners.get(id);
                if (statusListener != null) {
                    statusListener.onRemoved();
                }

                if (discovery != null && node != null) {
                    discovery.removeDiscoveredVM(vm);
                }
            });
        }

        private void fetchStatusUpdates4LXCs(ProxmoxNode node, Map<String, ProxmoxLxc> lastLxcStatesCopy)
                throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

            final ProxmoxDiscoveryService discovery = discoveryService;
            for (ProxmoxLxc lxc : getApi().getLXCs(node)) {
                String id = lxc.getLxcId();

                ProxmoxStatusChangedListener<ProxmoxLxc> lxcStatusListener = lxcStatusListeners.get(id);
                if (lxcStatusListener == null) {
                    logger.trace("LXC '{}' was added", id);

                    if (discovery != null && !lastLxcStates.containsKey(id)) {
                        discovery.notifyLxcDiscovered(lxc, node);
                    }

                    lastLxcStates.put(id, lxc);
                } else {
                    if (lxcStatusListener.onStateChanged(lxc)) {
                        lastLxcStates.put(id, lxc);
                    }
                }
                // LXC was handled, so remove it from the copy (if this node exists)
                lastLxcStatesCopy.remove(id);
            }

            // the remaining LXCs in lastLxcStatesCopy were not handled, thus have to be removed
            lastLxcStatesCopy.forEach((id, lxc) -> {
                logger.trace("LXC '{}' remove", id);
                lastLxcStates.remove(id);

                ProxmoxStatusChangedListener<ProxmoxLxc> statusListener = lxcStatusListeners.get(id);
                if (statusListener != null) {
                    statusListener.onRemoved();
                }

                if (discovery != null && node != null) {
                    discovery.removeDiscoveredLxc(lxc);
                }
            });
        }
    };

    public boolean tryResumeHostConnection() {
        return true;
    }

    public @Nullable ProxmoxNode getNodeById(String nodeId) {
        return lastNodeStates.get(nodeId);
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
    public Collection<@NonNull Class<? extends @NonNull ThingHandlerService>> getServices() {
        return Collections.singleton(ProxmoxDiscoveryService.class);
    }

    public void registerDiscoveryListener(ProxmoxDiscoveryService proxmoxDiscoveryService) {
        if (discoveryService == null) {
            discoveryService = proxmoxDiscoveryService;
        }
    }
}
