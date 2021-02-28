package org.openhab.binding.proxmox.internal.api.model;

public class ProxmoxNode {
    private String id;
    private String type;
    private String node;
    private NodeStatus status;
    private int maxmem;
    private int uptime;
    private int disk;
    private int maxcpu;
    private int maxdisk;
    private float cpu;
    private int mem;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the node
     */
    public String getNode() {
        return node;
    }

    /**
     * @return the status
     */
    public NodeStatus getStatus() {
        return status;
    }

    /**
     * @return the maxmem
     */
    public int getMaxmem() {
        return maxmem;
    }

    /**
     * @return the uptime
     */
    public int getUptime() {
        return uptime;
    }

    /**
     * @return the disk
     */
    public int getDisk() {
        return disk;
    }

    /**
     * @return the maxcpu
     */
    public int getMaxcpu() {
        return maxcpu;
    }

    /**
     * @return the maxdisk
     */
    public int getMaxdisk() {
        return maxdisk;
    }

    /**
     * @return the cpu
     */
    public float getCpu() {
        return cpu;
    }

    /**
     * @return the mem
     */
    public int getMem() {
        return mem;
    }
}
