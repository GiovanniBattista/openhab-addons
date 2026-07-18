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
package org.openhab.binding.proxmox.internal.discovery;

import static org.openhab.binding.proxmox.internal.ProxmoxBindingConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.proxmox.internal.ProxmoxBindingConstants;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApi;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxLxc;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxNode;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVm;
import org.openhab.binding.proxmox.internal.handler.ProxmoxHostBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProxmoxDiscoveryService
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
public class ProxmoxDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    public static final Set<ThingTypeUID> DISCOVERABLE_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Set.of(THING_TYPE_NODE, THING_TYPE_VM, THING_TYPE_LXC));

    private static final int TIMEOUT = 5;

    private final Logger logger = LoggerFactory.getLogger(ProxmoxDiscoveryService.class);

    private @Nullable ProxmoxHostBridgeHandler bridgeHandler;

    public ProxmoxDiscoveryService() {
        super(DISCOVERABLE_THING_TYPES_UIDS, TIMEOUT);
    }

    @Override
    protected void startScan() {
        ProxmoxHostBridgeHandler handler = bridgeHandler;
        if (handler == null || handler.getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }

        List<ProxmoxNode> nodes = discoverNodes(handler);
        discoverVMs(handler, nodes);
        discoverLXCs(handler, nodes);
    }

    private List<ProxmoxNode> discoverNodes(ProxmoxHostBridgeHandler handler) {
        List<ProxmoxNode> nodes = Collections.emptyList();
        ProxmoxVEApi api = handler.getApi();
        if (api == null) {
            return nodes;
        }
        try {
            nodes = api.getNodes();
            for (ProxmoxNode node : nodes) {
                notifyNodeDiscovered(node);
            }
        } catch (ProxmoxApiCommunicationException | ProxmoxApiConfigurationException e) {
            logger.debug("Could not discover nodes: {}", e.getMessage(), e);
        }
        return nodes;
    }

    public void notifyNodeDiscovered(ProxmoxNode node) {
        ProxmoxHostBridgeHandler handler = bridgeHandler;
        String nodeName = node.getNode();
        if (handler == null || nodeName == null) {
            return;
        }

        ThingUID bridgeUID = handler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_NODE, bridgeUID, nodeName);

        String type = node.getType();
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_NODE_NAME, nodeName);
        properties.put(PROPERTY_NODE_TYPE, type != null ? type : "");

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                .withLabel("Proxmox Node: " + nodeName).withProperties(properties)
                .withRepresentationProperty(PROPERTY_NODE_NAME).build();
        thingDiscovered(result);

        logger.debug("Discovered node '{}'", nodeName);
    }

    public void removeDiscoveredNode(ProxmoxNode node) {
        ProxmoxHostBridgeHandler handler = bridgeHandler;
        String nodeName = node.getNode();
        if (handler == null || nodeName == null) {
            return;
        }

        ThingUID bridgeUID = handler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_NODE, bridgeUID, nodeName);
        thingRemoved(uid);

        logger.debug("Removed discovered node '{}'", nodeName);
    }

    private void discoverVMs(ProxmoxHostBridgeHandler handler, List<ProxmoxNode> nodes) {
        ProxmoxVEApi api = handler.getApi();
        if (api == null) {
            return;
        }
        for (ProxmoxNode node : nodes) {
            try {
                List<ProxmoxVm> vms = api.getVMs(node);

                for (ProxmoxVm vm : vms) {
                    notifyVmDiscovered(vm, node);
                }
            } catch (ProxmoxApiCommunicationException | ProxmoxApiConfigurationException e) {
                logger.debug("Could not discover vms: {}", e.getMessage(), e);
            }
        }
    }

    public void notifyVmDiscovered(ProxmoxVm vm, ProxmoxNode node) {
        ProxmoxHostBridgeHandler handler = bridgeHandler;
        String vmId = vm.getVmid();
        String nodeName = node.getNode();
        if (handler == null || vmId == null || nodeName == null) {
            return;
        }

        ThingUID bridgeUID = handler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_VM, bridgeUID, vmId);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_VM_ID, vmId);
        properties.put(PROPERTY_VM_NODE, nodeName);

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                .withLabel("Proxmox VM: " + vm.getName()).withProperties(properties)
                .withRepresentationProperty(PROPERTY_VM_ID).build();
        thingDiscovered(result);

        logger.debug("Discovered VM '{}' with id '{}' on node '{}'", vm.getName(), vmId, nodeName);
    }

    public void removeDiscoveredVM(ProxmoxVm vm) {
        ProxmoxHostBridgeHandler handler = bridgeHandler;
        String vmId = vm.getVmid();
        if (handler == null || vmId == null) {
            return;
        }

        ThingUID bridgeUID = handler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_VM, bridgeUID, vmId);
        thingRemoved(uid);

        logger.debug("Removed discovered VM '{}' with id '{}'", vm.getName(), vmId);
    }

    private void discoverLXCs(ProxmoxHostBridgeHandler handler, List<ProxmoxNode> nodes) {
        ProxmoxVEApi api = handler.getApi();
        if (api == null) {
            return;
        }
        for (ProxmoxNode node : nodes) {
            try {
                List<ProxmoxLxc> lxcs = api.getLXCs(node);

                for (ProxmoxLxc lxc : lxcs) {
                    notifyLxcDiscovered(lxc, node);
                }
            } catch (ProxmoxApiCommunicationException | ProxmoxApiConfigurationException e) {
                logger.debug("Could not discover lxcs: {}", e.getMessage(), e);
            }
        }
    }

    public void notifyLxcDiscovered(ProxmoxLxc lxc, ProxmoxNode node) {
        ProxmoxHostBridgeHandler handler = bridgeHandler;
        String lxcId = lxc.getLxcId();
        String nodeName = node.getNode();
        if (handler == null || lxcId == null || nodeName == null) {
            return;
        }

        ThingUID bridgeUID = handler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_LXC, bridgeUID, lxcId);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_LXC_ID, lxcId);
        properties.put(PROPERTY_LXC_NODE, nodeName);

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                .withLabel("Proxmox LXC: " + lxc.getName()).withProperties(properties)
                .withRepresentationProperty(PROPERTY_LXC_ID).build();
        thingDiscovered(result);

        logger.debug("Discovered LXC '{}' with id '{}' on node '{}'", lxc.getName(), lxcId, nodeName);
    }

    public void removeDiscoveredLxc(ProxmoxLxc lxc) {
        ProxmoxHostBridgeHandler handler = bridgeHandler;
        String lxcId = lxc.getLxcId();
        if (handler == null || lxcId == null) {
            return;
        }

        ThingUID bridgeUID = handler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_LXC, bridgeUID, lxcId);
        thingRemoved(uid);

        logger.debug("Removed discovered LXC '{}' with id '{}'", lxc.getName(), lxcId);
    }

    @Override
    public void activate() {
        super.activate(null);

        ProxmoxHostBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            handler.registerDiscoveryListener(this);
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof ProxmoxHostBridgeHandler) {
            bridgeHandler = (ProxmoxHostBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }
}
