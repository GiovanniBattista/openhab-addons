package org.openhab.binding.proxmox.internal.api;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.proxmox.internal.api.auth.Authorization;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiCommunicationException;
import org.openhab.binding.proxmox.internal.api.exception.ProxmoxApiConfigurationException;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxLxc;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxNode;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxNodeStatus;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVersion;
import org.openhab.binding.proxmox.internal.api.model.ProxmoxVm;
import org.openhab.binding.proxmox.internal.api.model.StatusCommand;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class ProxmoxVEApi {

    private final ProxmoxVEApiContext context;
    private final Authorization auth;
    private final ProxmoxRequestHelper requestHelper;

    public ProxmoxVEApi(ProxmoxVEApiContext context, Authorization auth) {
        this.context = context;
        this.auth = auth;
        this.requestHelper = ProxmoxRequestHelper.of(context);
    }

    /**
     * Just a simple request which requires authentication to test the access to the API.
     *
     * @throws ProxmoxApiCommunicationException
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiInvalidResponseException
     */
    public ProxmoxVersion getVersion() throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        Request request = requestHelper.newGetRequest("version");
        auth.authenticate(request);

        return requestHelper.getContent(request, ProxmoxVersion.class);
    }

    public List<@NonNull ProxmoxNode> getNodes()
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newGetRequest("nodes");
        auth.authenticate(request);

        Type collectionType = new TypeToken<List<ProxmoxNode>>() {
        }.getType();
        return requestHelper.getContentAsList(request, collectionType);
    }

    public ProxmoxNodeStatus getNodeStatus(String id)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        Request request = requestHelper.newGetRequest("nodes/{0}/aplinfo", id);
        auth.authenticate(request);

        return requestHelper.getContent(request, ProxmoxNodeStatus.class);
    }

    public @NonNull List<@NonNull ProxmoxVm> getVMs(ProxmoxNode node)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newGetRequest("nodes/{0}/qemu", node.getNode());
        auth.authenticate(request);

        Type collectionType = new TypeToken<List<ProxmoxVm>>() {
        }.getType();
        return requestHelper.getContentAsList(request, collectionType);
    }

    public @NonNull List<@NonNull ProxmoxLxc> getLXCs(ProxmoxNode node)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {
        Request request = requestHelper.newGetRequest("nodes/{0}/lxc", node.getNode());
        auth.authenticate(request);

        Type collectionType = new TypeToken<List<ProxmoxLxc>>() {
        }.getType();
        return requestHelper.getContentAsList(request, collectionType);
    }

    /**
     * Reboot or shutdown a node.
     *
     * @param nodeId
     *            the id of the node to reboot or shutdown
     * @param command
     *            the reboot/shutdown command
     *
     * @throws ProxmoxApiCommunicationException
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiInvalidResponseException
     */
    public void rebootShutdownNode(String nodeId, StatusCommand command)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newPostRequest("nodes/{0}/status", nodeId);
        auth.authenticate(request);

        JsonObject object = new JsonObject();
        object.addProperty("node", nodeId);
        object.addProperty("command", Objects.toString(command));

        request.content(new StringContentProvider(object.toString()));

        requestHelper.sendRequest(request);
    }

    /**
     * Try to wake a node via 'wake on LAN' network packet.
     *
     * @param nodeId
     *            target node to wake up
     * @throws ProxmoxApiCommunicationException
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiInvalidResponseException
     */
    public void wakeonlanNode(String nodeId) throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newPostRequest("nodes/{0}/wakeonlan", nodeId);
        auth.authenticate(request);

        requestHelper.sendRequest(request);
    }

    /**
     * Shuts down the virtual machine.
     *
     * @param nodeName
     * @param vmId
     * @throws ProxmoxApiCommunicationException
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiInvalidResponseException
     */
    public void shutdownVm(String nodeName, String vmId)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newPostRequest("nodes/{0}/qemu/{1}/status/shutdown", nodeName, vmId);
        auth.authenticate(request);

        JsonObject object = new JsonObject();
        object.addProperty("node", nodeName);
        object.addProperty("vmid", Integer.valueOf(vmId));

        request.content(new StringContentProvider(object.toString()));

        requestHelper.sendRequest(request);
    }

    /**
     * Starts the virtual machine
     *
     * @param nodeName
     * @param vmId
     * @throws ProxmoxApiCommunicationException
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiInvalidResponseException
     */
    public void startVm(String nodeName, String vmId)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newPostRequest("nodes/{0}/qemu/{1}/status/start", nodeName, vmId);
        auth.authenticate(request);

        JsonObject object = new JsonObject();
        object.addProperty("node", nodeName);
        object.addProperty("vmid", Integer.valueOf(vmId));

        request.content(new StringContentProvider(object.toString()));

        requestHelper.sendRequest(request);
    }

    /**
     * Shutdown the container. This will trigger a clean shutdown of the container.
     *
     * @param nodeName
     * @param lxcId
     * @throws ProxmoxApiInvalidResponseException
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiCommunicationException
     */
    public void shutdownLxc(String nodeName, String lxcId)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newPostRequest("nodes/{0}/lxc/{1}/status/shutdown", nodeName, lxcId);
        auth.authenticate(request);

        JsonObject object = new JsonObject();
        object.addProperty("node", nodeName);
        object.addProperty("vmid", Integer.valueOf(lxcId));

        request.content(new StringContentProvider(object.toString()));

        requestHelper.sendRequest(request);
    }

    /**
     * Start the container
     *
     * @param nodeName
     * @param lxcId
     * @throws ProxmoxApiInvalidResponseException
     * @throws ProxmoxApiConfigurationException
     * @throws ProxmoxApiCommunicationException
     */
    public void startLxc(String nodeName, String lxcId)
            throws ProxmoxApiCommunicationException, ProxmoxApiConfigurationException {

        Request request = requestHelper.newPostRequest("nodes/{0}/lxc/{1}/status/start", nodeName, lxcId);
        auth.authenticate(request);

        JsonObject object = new JsonObject();
        object.addProperty("node", nodeName);
        object.addProperty("vmid", Integer.valueOf(lxcId));

        request.content(new StringContentProvider(object.toString()));

        requestHelper.sendRequest(request);
    }
}
