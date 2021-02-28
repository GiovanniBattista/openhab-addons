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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApi;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVm;
import org.openhab.binding.proxmox.internal.api.model.VmStatus;
import org.openhab.binding.proxmox.internal.config.ProxmoxVmConfiguration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Zupan - Initial contribution
 */
public class ProxmoxVmHandler extends BaseThingHandler implements ProxmoxStatusChangedListener<ProxmoxVm> {

    private final Logger logger = LoggerFactory.getLogger(ProxmoxVmHandler.class);

    private ProxmoxVmConfiguration config;
    private String nodeName;
    private String vmId;

    public ProxmoxVmHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing vm handler.");
        updateStatus(ThingStatus.UNKNOWN);

        config = getConfigAs(ProxmoxVmConfiguration.class);

        Bridge bridge = getBridge();
        initializeVm(bridge != null ? bridge.getStatus() : null);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.info("Bridge status changed to {}", bridgeStatusInfo);
        initializeVm(bridgeStatusInfo.getStatus());
    }

    private void initializeVm(@Nullable ThingStatus bridgeStatus) {
        logger.debug("initializeVm: thing{} bridge status {}", getThing().getUID(), bridgeStatus);

        nodeName = getThing().getProperties().get(PROPERTY_VM_NODE);
        if (nodeName == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Node name was not set as property");
            return;
        }

        vmId = getThing().getProperties().get(PROPERTY_VM_ID);
        if (vmId == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "VM ID was not set as property");
            return;
        }

        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (bridgeHandler != null) {
            if (bridgeStatus == ThingStatus.ONLINE) {
                bridgeHandler.registerVmStatusChangeListener(vmId, this);
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

        if (vmId == null) {
            logger.debug("The VM was not initialized properly: Missing VM ID. Cannot handle command!");
            return;
        }

        if (nodeName == null) {
            logger.debug("The VM was not initialized properly: Missing node name. Cannot handle command!");
            return;
        }

        ProxmoxVm vm = bridgeHandler.getVmById(vmId);
        if (vm == null) {
            logger.debug("The VM is not known to the bridge. Cannot handle command!");
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
                            getApi().shutdownVm(nodeName, vmId);
                        } else if (powerState == OnOffType.ON) {
                            getApi().startVm(nodeName, vmId);
                        }
                    }
            }
        } catch (ProxmoxApiCommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
        } catch (ProxmoxApiConfigurationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.getMessage());
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        logger.debug("VM was disposed. Unregister listener.");
        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (vmId != null && bridgeHandler != null) {
            bridgeHandler.unregisterVmStatusChangeListener(vmId);
            vmId = null;
            nodeName = null;
        }
    }

    private ProxmoxVEApi getApi() {
        return ProxmoxHostBridgeHandlerHelper.getApi(getBridge());
    }

    // ========== ProxmoxStatusChangedListener implementation ===============================
    @Override
    public boolean onStateChanged(ProxmoxVm vm) {
        logger.trace("onStateChanged was called!");

        // TODO Properly handle onStateChanged
        updateState(CHANNEL_POWER, OnOffType.from(vm.getStatus() == VmStatus.RUNNING));

        return true;
    }

    @Override
    public void onAdded(ProxmoxVm vm) {
        onStateChanged(vm);
    }

    @Override
    public void onRemoved() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "VM was removed");
    }

    @Override
    public void onGone() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "VM gone");
    }
}
