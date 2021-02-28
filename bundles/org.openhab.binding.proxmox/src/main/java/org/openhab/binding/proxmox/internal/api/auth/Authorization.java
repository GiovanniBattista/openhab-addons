package org.openhab.binding.proxmox.internal.api.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;

/**
 * TODO
 *
 * @author Daniel Zupan
 */
@NonNullByDefault
public interface Authorization {

    void authenticate(Request request) throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException;
}
