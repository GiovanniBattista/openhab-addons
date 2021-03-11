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
package org.openhab.binding.proxmox.internal.handler;

import static org.openhab.binding.proxmox.internal.ProxmoxBindingConstants.*;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.proxmox.internal.ProxmoxBindingConstants;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApi;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.NodeStatus;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxNode;
import org.openhab.binding.proxmox.internal.api.model.StatusCommand;
import org.openhab.binding.proxmox.internal.config.ProxmoxNodeConfiguration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Zupan - Initial contribution
 *
 */
public class ProxmoxNodeHandler extends BaseThingHandler implements ProxmoxStatusChangedListener<ProxmoxNode> {

    private final Logger logger = LoggerFactory.getLogger(ProxmoxNodeHandler.class);

    private ProxmoxNodeConfiguration config;
    private String nodeName;

    /**
     * @param thing
     */
    public ProxmoxNodeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing node handler.");
        updateStatus(ThingStatus.UNKNOWN);

        config = getConfigAs(ProxmoxNodeConfiguration.class);

        Bridge bridge = getBridge();
        initializeNode(bridge != null ? bridge.getStatus() : null);
    }

    @Override
    public void bridgeStatusChanged(@NonNull ThingStatusInfo bridgeStatusInfo) {
        logger.debug("Bridge status changed to {}.", bridgeStatusInfo);
        initializeNode(bridgeStatusInfo.getStatus());
    }

    private void initializeNode(@Nullable ThingStatus bridgeStatus) {
        logger.debug("initializeNode: thing {} bridge status {}", getThing().getUID(), bridgeStatus);

        nodeName = getThing().getProperties().get(PROPERTY_NODE_NAME);
        if (nodeName == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Node name was not set as property!");
            return;
        }

        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (bridgeHandler != null) {
            if (bridgeStatus == ThingStatus.ONLINE) {
                bridgeHandler.registerNodeStatusChangeListener(nodeName, this);
                // ProxmoxNode node = bridgeHandler.getNodeById(nodeName);
                // TODO initializeProperties(node);
                // TODO initializeCapabilities(node);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (bridgeHandler == null) {
            logger.warn("Bridge handler was not found. Cannot handle command without bridge!");
            return;
        }

        if (nodeName == null) {
            logger.debug("The node was not initialized properly. Cannot handle command!");
            return;
        }

        ProxmoxNode node = bridgeHandler.getNodeById(nodeName);
        if (node == null) {
            logger.debug("The node is not known to the bridge. Cannot handle command!");
            return;
        }

        if (command == RefreshType.REFRESH) {
            refreshChannelStates();
            return;
        }

        try {
            String channel = channelUID.getId();
            switch (channel) {
                case CHANNEL_POWER:
                    logger.trace("CHANNEL_POWER was changed to {}", command);
                    if (command instanceof OnOffType) {
                        OnOffType powerState = (OnOffType) command;
                        if (powerState == OnOffType.OFF) {
                            // node was requested to poweroff, therefore use POST /nodes/{node}/status to shut down the
                            // node
                            getApi().rebootShutdownNode(nodeName, StatusCommand.SHUTDOWN);
                        } else if (powerState == OnOffType.ON) {
                            getApi().wakeonlanNode(nodeName);
                        }
                    }
                    break;
            }
        } catch (ProxmoxApiCommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
        } catch (ProxmoxApiConfigurationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
        }
    }

    private void refreshChannelStates() {
        if (getThing().getStatus() == ThingStatus.OFFLINE) {
            updateState(ProxmoxBindingConstants.CHANNEL_POWER, OnOffType.OFF);
        }
    }

    private ProxmoxVEApi getApi() {
        return ProxmoxHostBridgeHandlerHelper.getApi(getBridge());
    }

    @Override
    public void dispose() {
        super.dispose();

        logger.debug("Node was disposed. Unregister listener.");
        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (nodeName != null && bridgeHandler != null) {
            bridgeHandler.unregisterNodeStatusChangeListener(nodeName);
            nodeName = null;
        }
    }

    // @Override
    // public Collection<@NonNull Class<? extends @NonNull ThingHandlerService>> getServices() {
    // return Collections.singleton(ProxmoxNodeAction.class);
    // }

    // ========== ProxmoxStatusChangedListener implementation ===============================

    @Override
    public boolean onStateChanged(ProxmoxNode node) {
        logger.trace("onStateChanged was called!");

        updateState(CHANNEL_POWER, OnOffType.from(node.getStatus() == NodeStatus.ONLINE));

        return true;
    }

    @Override
    public void onAdded(ProxmoxNode node) {
        onStateChanged(node);
    }

    @Override
    public void onRemoved() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "@text/offline.node-removed");
    }

    @Override
    public void onGone() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "@text/offline.node-gone");
    }
}
