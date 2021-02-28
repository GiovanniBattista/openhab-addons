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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.proxmox.internal.discovery.ProxmoxDiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ProxmoxHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Daniel Zupan - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.proxmox", service = ThingHandlerFactory.class)
public class ProxmoxHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Set.of(THING_TYPE_HOST, THING_TYPE_NODE, THING_TYPE_VM, THING_TYPE_LXC));

    private final Logger logger = LoggerFactory.getLogger(ProxmoxHandlerFactory.class);
    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private final HttpClient httpClient;

    @Activate
    public ProxmoxHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        // this.httpClient = httpClientFactory.getCommonHttpClient(); // cannot be used with self signed/untrusted
        // certificates
        this.httpClient = new HttpClient(new SslContextFactory.Client(true));
        try {
            this.httpClient.start();
        } catch (Exception ex) {
            logger.warn("Failed to start insecure http client: {}", ex.getMessage());
            throw new IllegalStateException("Could not create HttpClient instance", ex);
        }
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_HOST.equals(thingTypeUID)) {
            return new ProxmoxHostBridgeHandler((Bridge) thing, httpClient);
            // registerProxmoxHistoryService(hostHandler);
        } else if (THING_TYPE_NODE.equals(thingTypeUID)) {
            return new ProxmoxNodeHandler(thing);
        } else if (THING_TYPE_VM.equals(thingTypeUID)) {
            return new ProxmoxVmHandler(thing);
        } else if (THING_TYPE_LXC.equals(thingTypeUID)) {
            return new ProxmoxLxcHandler(thing);
        }

        return null;
    }

    // private synchronized void registerProxmoxHistoryService(ProxmoxHostBridgeHandler hostHandler) {
    // ProxmoxDiscoveryService discoveryService = new ProxmoxDiscoveryService(hostHandler);
    // ServiceRegistration<?> serviceRegistration = bundleContext.registerService(DiscoveryService.class.getName(),
    // discoveryService, new Hashtable<>());
    // discoveryService.activate();
    // this.discoveryServiceRegs.put(hostHandler.getThing().getUID(), serviceRegistration);
    // }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        super.removeHandler(thingHandler);

        if (thingHandler instanceof ProxmoxHostBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                ProxmoxDiscoveryService service = (ProxmoxDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
                if (service != null) {
                    service.deactivate();
                }
            }
        }
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        super.deactivate(componentContext);

        try {
            httpClient.stop();
        } catch (Exception ex) {
            logger.warn("Failed to stop HttpCLient instance: {}", ex.getMessage());
        }
    }
}
