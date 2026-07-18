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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApi;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.NodeStatus;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxNode;
import org.openhab.binding.proxmox.internal.api.model.StatusCommand;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ProxmoxNodeHandler} is responsible for handling commands and state updates of a Proxmox node.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxNodeHandler extends BaseThingHandler implements ProxmoxStatusChangedListener<ProxmoxNode> {

    private final Logger logger = LoggerFactory.getLogger(ProxmoxNodeHandler.class);

    // The minimum time in ms to skip the next update cycle if a command has been issued.
    private static final int MIN_SKIP_UPDATE_CYCLE_TIME = 30000;

    private volatile @Nullable String nodeName;

    private long endSkipTime = 0L;

    public ProxmoxNodeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing node handler.");
        updateStatus(ThingStatus.UNKNOWN);

        Bridge bridge = getBridge();
        initializeNode(bridge != null ? bridge.getStatus() : null);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("Bridge status changed to {}.", bridgeStatusInfo);
        initializeNode(bridgeStatusInfo.getStatus());
    }

    private void initializeNode(@Nullable ThingStatus bridgeStatus) {
        logger.debug("initializeNode: thing {} bridge status {}", getThing().getUID(), bridgeStatus);

        String node = getThing().getProperties().get(PROPERTY_NODE_NAME);
        nodeName = node;
        if (node == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Node name was not set as property!");
            return;
        }

        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (bridgeHandler != null) {
            if (bridgeStatus == ThingStatus.ONLINE) {
                bridgeHandler.registerNodeStatusChangeListener(node, this);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                updateState(CHANNEL_POWER, OnOffType.OFF);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
            updateState(CHANNEL_POWER, OnOffType.OFF);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (bridgeHandler == null) {
            logger.warn("Bridge handler was not found. Cannot handle command without bridge!");
            return;
        }

        String node = nodeName;
        if (node == null) {
            logger.debug("The node was not initialized properly. Cannot handle command!");
            return;
        }

        String channel = channelUID.getId();
        ProxmoxNode proxmoxNode = bridgeHandler.getNodeById(node);
        if (proxmoxNode == null && !CHANNEL_POWER.equals(channel)) {
            logger.debug("The node is not known to the bridge. Cannot handle command!");
            return;
        }

        if (command instanceof RefreshType) {
            refreshChannelStates();
            return;
        }

        ProxmoxVEApi api = getApi();
        if (api == null) {
            logger.debug("The API is not available. Cannot handle command!");
            return;
        }

        try {
            if (CHANNEL_POWER.equals(channel) && command instanceof OnOffType powerState) {
                logger.trace("CHANNEL_POWER was changed to {}", command);
                if (powerState == OnOffType.OFF) {
                    // node was requested to power off, therefore use POST /nodes/{node}/status to shut down the node
                    api.rebootShutdownNode(node, StatusCommand.SHUTDOWN);
                } else {
                    if (getThing().getStatus() == ThingStatus.OFFLINE) {
                        // try to wake the host/bridge via Wake on LAN
                        bridgeHandler.wakeOnLan(node);
                    } else {
                        api.wakeonlanNode(node);
                    }
                }
                updateState(channel, powerState);
                skipNextUpdateCycle();
            }
        } catch (ProxmoxApiCommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
        } catch (ProxmoxApiConfigurationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
        }
    }

    private void refreshChannelStates() {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            updateState(CHANNEL_POWER, OnOffType.OFF);
            return;
        }

        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        String node = nodeName;
        if (bridgeHandler != null && node != null) {
            ProxmoxNode proxmoxNode = bridgeHandler.getNodeById(node);
            if (proxmoxNode != null) {
                updateChannels(proxmoxNode);
            }
        }
    }

    private void updateChannels(ProxmoxNode node) {
        NodeStatus status = node.getStatus();
        updateState(CHANNEL_STATUS, status != null ? new StringType(status.toString()) : UnDefType.UNDEF);
        updateState(CHANNEL_POWER, OnOffType.from(status == NodeStatus.ONLINE));
        updateState(CHANNEL_CPU_LOAD, new QuantityType<>(node.getCpu() * 100.0, Units.PERCENT));
        updateState(CHANNEL_MEMORY_USED, new QuantityType<>(node.getMem(), Units.BYTE));
        updateState(CHANNEL_MEMORY_TOTAL, new QuantityType<>(node.getMaxmem(), Units.BYTE));
        updateState(CHANNEL_DISK_USED, new QuantityType<>(node.getDisk(), Units.BYTE));
        updateState(CHANNEL_DISK_TOTAL, new QuantityType<>(node.getMaxdisk(), Units.BYTE));
        updateState(CHANNEL_UPTIME, new QuantityType<>(node.getUptime(), Units.SECOND));
    }

    private void skipNextUpdateCycle() {
        endSkipTime = System.currentTimeMillis() + MIN_SKIP_UPDATE_CYCLE_TIME;
    }

    private @Nullable ProxmoxVEApi getApi() {
        return ProxmoxHostBridgeHandlerHelper.getApi(getBridge());
    }

    @Override
    public void dispose() {
        super.dispose();

        logger.debug("Node was disposed. Unregister listener.");
        String node = nodeName;
        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (node != null && bridgeHandler != null) {
            bridgeHandler.unregisterNodeStatusChangeListener(node);
            nodeName = null;
        }
    }

    // ========== ProxmoxStatusChangedListener implementation ===============================

    @Override
    public boolean onStateChanged(ProxmoxNode node) {
        logger.trace("onStateChanged was called!");

        if (System.currentTimeMillis() <= endSkipTime) {
            logger.debug("Skipping update cycle for id: {}", nodeName);
            return false;
        }

        updateChannels(node);
        return true;
    }

    @Override
    public void onAdded(ProxmoxNode node) {
        onStateChanged(node);
    }

    @Override
    public void onRemoved() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "@text/offline.node-removed");
    }

    @Override
    public void onGone() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "@text/offline.node-gone");
    }
}
