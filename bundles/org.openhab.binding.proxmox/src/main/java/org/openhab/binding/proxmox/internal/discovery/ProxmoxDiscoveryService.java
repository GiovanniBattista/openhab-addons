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

@NonNullByDefault
public class ProxmoxDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {

    public static final Set<ThingTypeUID> DISCOVERABLE_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Set.of(THING_TYPE_NODE, THING_TYPE_VM, THING_TYPE_LXC));

    private static final int TIMEOUT = 5;
    private static final long REFRESH = 600;

    private final Logger logger = LoggerFactory.getLogger(ProxmoxDiscoveryService.class);

    // private ScheduledFuture<?> discoveryFuture;

    private @Nullable ProxmoxHostBridgeHandler bridgeHandler;

    public ProxmoxDiscoveryService() {
        super(DISCOVERABLE_THING_TYPES_UIDS, TIMEOUT);
    }

    @Override
    protected void startScan() {
        if (bridgeHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            return; // bridge not initialized yet so return here
        }

        List<ProxmoxNode> nodes = discoverNodes();
        discoverVMs(nodes);
        discoverLXCs(nodes);
    }

    private List<ProxmoxNode> discoverNodes() {
        List<ProxmoxNode> nodes = Collections.emptyList();
        try {
            nodes = bridgeHandler.getApi().getNodes();
            for (ProxmoxNode node : nodes) {
                notifyNodeDiscovered(node);
            }
        } catch (ProxmoxApiCommunicationException | ProxmoxApiConfigurationException e) {
            logger.debug("Could not discover nodes: {}", e.getMessage(), e);
        }
        return nodes;
    }

    public void notifyNodeDiscovered(ProxmoxNode node) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_NODE, bridgeUID, node.getNode());

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_NODE_NAME, node.getNode());
        properties.put(PROPERTY_NODE_TYPE, node.getType());

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                .withLabel("Proxmox Node: " + node.getNode()).withProperties(properties)
                .withRepresentationProperty(PROPERTY_NODE_NAME).build();
        thingDiscovered(result);

        logger.debug("Discovered node '{}'", node.getNode());
    }

    public void removeDiscoveredNode(ProxmoxNode node) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    private void discoverVMs(List<ProxmoxNode> nodes) {
        for (ProxmoxNode node : nodes) {
            try {
                List<ProxmoxVm> vms = bridgeHandler.getApi().getVMs(node);

                for (ProxmoxVm vm : vms) {
                    notifyVmDiscovered(vm, node);
                }
            } catch (ProxmoxApiCommunicationException | ProxmoxApiConfigurationException e) {
                logger.debug("Could not disover vms: {}", e.getMessage(), e);
            }
        }
    }

    public void notifyVmDiscovered(ProxmoxVm vm, ProxmoxNode node) {

        String vmId = vm.getVmid();

        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_VM, bridgeUID, vmId);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_VM_ID, vmId);
        properties.put(PROPERTY_VM_NODE, node.getNode());

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                .withLabel("Proxmox VM: " + vm.getName()).withProperties(properties)
                .withRepresentationProperty(PROPERTY_VM_ID).build();
        thingDiscovered(result);

        logger.debug("Discovered VM '{}' with id '{}' on node '{}'", vm.getName(), vm.getVmid(), node.getNode());
    }

    public void removeDiscoveredVM(ProxmoxVm vm) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    private void discoverLXCs(List<ProxmoxNode> nodes) {
        for (ProxmoxNode node : nodes) {
            try {
                List<ProxmoxLxc> lxcs = bridgeHandler.getApi().getLXCs(node);

                for (ProxmoxLxc lxc : lxcs) {
                    notifyLxcDiscovered(lxc, node);
                }
            } catch (ProxmoxApiCommunicationException | ProxmoxApiConfigurationException e) {
                logger.debug("Could not disover lxcs: {}", e.getMessage(), e);
            }
        }
    }

    public void notifyLxcDiscovered(ProxmoxLxc lxc, ProxmoxNode node) {

        String lxcId = lxc.getLxcId();

        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingUID uid = new ThingUID(ProxmoxBindingConstants.THING_TYPE_LXC, bridgeUID, lxcId);

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_LXC_ID, lxcId);
        properties.put(PROPERTY_LXC_NODE, node.getNode());

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                .withLabel("Proxmox LXC: " + lxc.getName()).withProperties(properties)
                .withRepresentationProperty(PROPERTY_LXC_ID).build();
        thingDiscovered(result);

        logger.debug("Discovered VM '{}' with id '{}' on node '{}'", lxc.getName(), lxc.getLxcId(), node.getNode());
    }

    public void removeDiscoveredLxc(ProxmoxLxc lxc) {
        throw new UnsupportedOperationException("not yet implemented!");
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
