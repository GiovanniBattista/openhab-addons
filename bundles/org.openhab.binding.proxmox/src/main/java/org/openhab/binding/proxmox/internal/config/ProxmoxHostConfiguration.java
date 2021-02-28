package org.openhab.binding.proxmox.internal.config;

public class ProxmoxHostConfiguration {
    private String baseUrl;
    private String username;
    private String password;
    private int pollingInterval;

    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the pollingInterval
     */
    public int getPollingInterval() {
        return pollingInterval;
    }
}
