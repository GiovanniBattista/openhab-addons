package org.openhab.binding.proxmox.internal.api;

import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.proxmox.internal.api.auth.Authorization;
import org.openhab.binding.proxmox.internal.api.auth.ProxmoxAuthentication;
import org.openhab.binding.proxmox.internal.config.ProxmoxHostConfiguration;

import com.google.gson.Gson;

public class ProxmoxVEApiFactory {
    public static ProxmoxVEApi create(ProxmoxHostConfiguration config, HttpClient httpClient) {

        // TODO use composition rather than inheritance
        Gson gson = GsonBuilderFactory.defaultBuilder().create();
        ProxmoxVEApiContext context = ProxmoxVEApiContext.of(config, httpClient, gson);

        Authorization auth = new ProxmoxAuthentication(context);
        return new ProxmoxVEApi(context, auth);
    }
}
