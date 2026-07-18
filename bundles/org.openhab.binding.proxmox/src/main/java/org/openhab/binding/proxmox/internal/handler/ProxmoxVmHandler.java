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
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVm;
import org.openhab.binding.proxmox.internal.api.model.VmStatus;
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
 * The {@link ProxmoxVmHandler} is responsible for handling commands and state updates of a Proxmox QEMU VM.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxVmHandler extends BaseThingHandler implements ProxmoxStatusChangedListener<ProxmoxVm> {

    private final Logger logger = LoggerFactory.getLogger(ProxmoxVmHandler.class);

    // The minimum time in ms to skip the next update cycle if a command has been issued.
    private static final int MIN_SKIP_UPDATE_CYCLE_TIME = 10000;

    private volatile @Nullable String nodeName;
    private volatile @Nullable String vmId;

    private long endSkipTime = 0L;

    public ProxmoxVmHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing vm handler.");
        updateStatus(ThingStatus.UNKNOWN);

        Bridge bridge = getBridge();
        initializeVm(bridge != null ? bridge.getStatus() : null);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("Bridge status changed to {}", bridgeStatusInfo);
        initializeVm(bridgeStatusInfo.getStatus());
    }

    private void initializeVm(@Nullable ThingStatus bridgeStatus) {
        logger.debug("initializeVm: thing {} bridge status {}", getThing().getUID(), bridgeStatus);

        String node = getThing().getProperties().get(PROPERTY_VM_NODE);
        nodeName = node;
        if (node == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Node name was not set as property");
            return;
        }

        String id = getThing().getProperties().get(PROPERTY_VM_ID);
        vmId = id;
        if (id == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "VM ID was not set as property");
            return;
        }

        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (bridgeHandler != null) {
            if (bridgeStatus == ThingStatus.ONLINE) {
                bridgeHandler.registerVmStatusChangeListener(id, this);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                updateState(CHANNEL_POWER, OnOffType.OFF);
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

        String id = vmId;
        String node = nodeName;
        if (id == null || node == null) {
            logger.debug("The VM was not initialized properly. Cannot handle command!");
            return;
        }

        ProxmoxVm vm = bridgeHandler.getVmById(id);
        if (vm == null) {
            logger.debug("The VM is not known to the bridge. Cannot handle command!");
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
            String channel = channelUID.getId();
            if (CHANNEL_POWER.equals(channel) && command instanceof OnOffType powerState) {
                logger.trace("CHANNEL_POWER was changed to {}", command);
                if (powerState == OnOffType.OFF) {
                    api.shutdownVm(node, id);
                } else {
                    api.startVm(node, id);
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
        String id = vmId;
        if (bridgeHandler != null && id != null) {
            ProxmoxVm vm = bridgeHandler.getVmById(id);
            if (vm != null) {
                updateChannels(vm);
            }
        }
    }

    private void updateChannels(ProxmoxVm vm) {
        VmStatus status = vm.getStatus();
        updateState(CHANNEL_STATUS, status != null ? new StringType(status.toString()) : UnDefType.UNDEF);
        updateState(CHANNEL_POWER, OnOffType.from(status == VmStatus.RUNNING));
        updateState(CHANNEL_CPU_LOAD, new QuantityType<>(vm.getCpu() * 100.0, Units.PERCENT));
        updateState(CHANNEL_MEMORY_USED, new QuantityType<>(vm.getMem(), Units.BYTE));
        updateState(CHANNEL_MEMORY_TOTAL, new QuantityType<>(vm.getMaxmem(), Units.BYTE));
        updateState(CHANNEL_DISK_TOTAL, new QuantityType<>(vm.getMaxdisk(), Units.BYTE));
        updateState(CHANNEL_UPTIME, new QuantityType<>(vm.getUptime(), Units.SECOND));
    }

    @Override
    public void dispose() {
        super.dispose();

        logger.debug("VM was disposed. Unregister listener.");
        String id = vmId;
        ProxmoxHostBridgeHandler bridgeHandler = ProxmoxHostBridgeHandlerHelper.getBridgeHandler(getBridge());
        if (id != null && bridgeHandler != null) {
            bridgeHandler.unregisterVmStatusChangeListener(id);
            vmId = null;
            nodeName = null;
        }
    }

    private void skipNextUpdateCycle() {
        endSkipTime = System.currentTimeMillis() + MIN_SKIP_UPDATE_CYCLE_TIME;
    }

    private @Nullable ProxmoxVEApi getApi() {
        return ProxmoxHostBridgeHandlerHelper.getApi(getBridge());
    }

    // ========== ProxmoxStatusChangedListener implementation ===============================
    @Override
    public boolean onStateChanged(ProxmoxVm vm) {
        logger.trace("onStateChanged was called!");

        if (System.currentTimeMillis() <= endSkipTime) {
            logger.debug("Skipping update cycle for id: {}", vmId);
            return false;
        }

        updateChannels(vm);
        return true;
    }

    @Override
    public void onAdded(ProxmoxVm vm) {
        onStateChanged(vm);
    }

    @Override
    public void onRemoved() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "@text/offline.vm-removed");
    }

    @Override
    public void onGone() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "@text/offline.vm-gone");
    }
}
