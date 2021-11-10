package org.openhab.binding.proxmox.internal.config;

public class ProxmoxHostConfiguration {
    private String baseUrl;
    private String username;
    private String password;
    private String macAddress;

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
     * @param macAddress the macAddress to set
     */
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * @return the macAddress
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * @return the pollingInterval
     */
    public int getPollingInterval() {
        return pollingInterval;
    }
}
