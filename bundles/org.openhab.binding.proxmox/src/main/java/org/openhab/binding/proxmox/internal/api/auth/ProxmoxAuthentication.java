package org.openhab.binding.proxmox.internal.api.auth;

import java.net.HttpCookie;
import java.time.LocalDateTime;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.proxmox.internal.api.ProxmoxRequestHelper;
import org.openhab.binding.proxmox.internal.api.ProxmoxVEApiContext;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.AccessTicketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 *
 *
 * @author Daniel Zupan
 */
public class ProxmoxAuthentication implements Authorization {

    private final Logger logger = LoggerFactory.getLogger(ProxmoxAuthentication.class);

    private final ProxmoxVEApiContext context;
    private final ProxmoxRequestHelper requestHelper;

    private AccessTicketResponse accessTicket;

    public ProxmoxAuthentication(ProxmoxVEApiContext context) {
        this.context = context;
        this.requestHelper = ProxmoxRequestHelper.of(context);
    }

    /**
     * Adds the authentication token as cookie to the given request.
     *
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiCommunicationException
     * @throws ProxmoxApiInvalidResponseException
     *
     * @see https://pve.proxmox.com/wiki/Proxmox_VE_API#Authentication
     */
    @Override
    public void authenticate(Request request)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        HttpCookie authCookie = new HttpCookie("PVEAuthCookie", getAuthToken());
        request.cookie(authCookie);

        if (isWriteRequest(request)) {
            // any write request must include the CSRFPreventionToken header
            request.header("CSRFPreventionToken", getCSRFPreventionToken());
        }
    }

    private String getAuthToken() throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        if (accessTicket == null || isExpired()) {
            initializeTokens();
        }
        return accessTicket.getTicket();
    }

    private String getCSRFPreventionToken() {
        return accessTicket.getCsrfPreventionToken(); // this token should already be the latest one
    }

    private void initializeTokens() throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        validateConfiguration();

        JsonObject content = new JsonObject();
        content.addProperty("username", context.getConfig().getUsername());
        content.addProperty("password", context.getConfig().getPassword());

        Request request = requestHelper.newPostRequest("/access/ticket")
                .content(new StringContentProvider(content.toString()));

        accessTicket = requestHelper.getContent(request, AccessTicketResponse.class);
    }

    private void validateConfiguration() throws ProxmoxApiConfigurationException {
        // TODO add regex validation for base url
        if (context.getConfig().getBaseUrl() == null || context.getConfig().getBaseUrl().isEmpty()) {
            throw new ProxmoxApiConfigurationException("Base URL is missing!");
        }
        if (context.getConfig().getUsername() == null || context.getConfig().getUsername().isEmpty()) {
            throw new ProxmoxApiConfigurationException("No username was provided!");
        }
        if (context.getConfig().getPassword() == null || context.getConfig().getPassword().isEmpty()) {
            throw new ProxmoxApiConfigurationException("No password was provided!");
        }
    }

    private boolean isExpired() {
        return accessTicket == null || accessTicket.getTokenExpiration().isBefore(LocalDateTime.now());
    }

    private boolean isWriteRequest(Request request) {
        return HttpMethod.POST.is(request.getMethod()) || HttpMethod.PUT.is(request.getMethod())
                || HttpMethod.DELETE.is(request.getMethod());
    }
}
