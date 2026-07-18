# Proxmox Binding

This binding integrates [Proxmox VE](https://www.proxmox.com/en/proxmox-virtual-environment) hosts into openHAB.

It connects to the Proxmox VE REST API, automatically discovers the nodes, virtual machines (QEMU) and containers (LXC) of a host and lets you:

- start and stop VMs and LXC containers,
- shut down or wake nodes (physical hosts), including Wake on LAN when the host is powered off, and
- monitor status, CPU load, memory, disk usage and uptime.

## Supported Things

| Thing type | Type   | Description                                                              |
|------------|--------|--------------------------------------------------------------------------|
| `host`     | Bridge | A Proxmox VE host. Holds the connection and polls the API.               |
| `node`     | Thing  | A node (physical host) of a Proxmox VE (cluster).                        |
| `vm`       | Thing  | A QEMU virtual machine running on a node.                               |
| `lxc`      | Thing  | A Linux container (LXC) running on a node.                              |

The Thing status reflects whether the binding can reach and manage the object, not whether the guest is powered on.
A node, VM or container that exists in Proxmox stays `ONLINE` even while it is stopped - its running state is exposed through the `power` and `status` channels.
A Thing only goes `OFFLINE` when its bridge is offline or when the object is removed from Proxmox.

## Discovery

Once a `host` bridge is configured and `ONLINE`, its nodes, VMs and containers are discovered automatically and appear in the inbox.
Newly created and removed guests are picked up on the next polling cycle, so background discovery keeps the inbox in sync without a manual scan.

## Bridge Configuration

The binding requires a Proxmox user with the following permissions:

```text
Start/stop nodes:    ["perm","/nodes/{node}",["Sys.PowerMgmt"]]
List VMs and LXCs:   ["perm","/vms/{vmid}",["VM.Audit"]]
Start/stop VMs/LXCs: ["perm","/vms/{vmid}",["VM.PowerMgmt"]]
```

All other operations do not require special permissions.
It is therefore recommended to create a dedicated user for the binding:

1. Open _Datacenter → Permissions → Users_ and add a new user (e.g. `openhab`).
   Note: for users in the Linux PAM realm, the user also needs to exist locally, see the [Proxmox user management wiki](https://pve.proxmox.com/wiki/User_Management).
1. Open _Datacenter → Permissions → Roles_ and create a new role (e.g. `openhab`) with the privileges `Sys.PowerMgmt`, `VM.Audit` and `VM.PowerMgmt`.
1. Open _Datacenter → Permissions_ and add a new permission for path `/`, the `openhab` user and the role created above.

### Authentication

The binding supports two authentication methods:

- **API token (recommended):** Open _Datacenter → Permissions → API Tokens_, create a token for the user above and copy the token id (e.g. `openhab@pam!mytoken`) and the secret shown once on creation into `apiTokenId` and `apiTokenSecret`. This avoids storing the account password in openHAB and needs no CSRF handling.
- **User name and password:** Set `username` (including the realm, e.g. `openhab@pam`) and `password`.

Provide either an API token or a user name and password. If both are set, the API token takes precedence.

### TLS

Proxmox VE uses a self-signed certificate by default, which the Java runtime does not trust.
In that case enable `trustAllCertificates` to skip certificate validation.
If the host presents a certificate trusted by the runtime (e.g. via a company CA or Let's Encrypt), leave it disabled.

### Parameters

The `host` bridge has the following configuration parameters:

| Parameter              | Required | Default | Description                                                                                                            |
|------------------------|----------|---------|----------------------------------------------------------------------------------------------------------------------|
| `baseUrl`              | yes      |         | Base URL of the Proxmox VE API, e.g. `https://pve:8006/`.                                                             |
| `username`             | no       |         | User name including the realm, e.g. `openhab@pam`. Required unless an API token is used.                              |
| `password`             | no       |         | Password of the API user. Required unless an API token is used.                                                      |
| `apiTokenId`           | no       |         | Full API token id including user and realm, e.g. `openhab@pam!mytoken`.                                               |
| `apiTokenSecret`       | no       |         | The token secret (UUID) shown once on creation.                                                                      |
| `trustAllCertificates` | no       | false   | Disable TLS certificate validation (needed for the default self-signed Proxmox certificate).                         |
| `macAddress`           | no       |         | MAC address of the host, used to power it on via Wake on LAN. If left empty, the binding tries to auto-detect it via ARP. |
| `pollingInterval`      | no       | 30      | Seconds between two polls of the Proxmox API.                                                                         |

## Thing Configuration

The `node`, `vm` and `lxc` things do not require any configuration.
They are identified by properties (node name and VM/container id) that are set automatically during discovery, so the recommended way to add them is via the inbox.

## Channels

All telemetry channels are read-only. The `power` channel is the only writable channel.

| Channel        | Type                | Description                                                                                                   |
|----------------|---------------------|-------------------------------------------------------------------------------------------------------------|
| `power`        | Switch              | `ON` starts the guest / wakes the node, `OFF` shuts it down. Reflects the running state while polling.       |
| `status`       | String              | The raw status reported by Proxmox VE (e.g. `running`, `stopped`, `online`, `offline`).                      |
| `cpu-load`     | Number:Dimensionless| Current CPU load in percent.                                                                                 |
| `memory-used`  | Number:DataAmount   | Currently used memory.                                                                                       |
| `memory-total` | Number:DataAmount   | Maximum available memory.                                                                                    |
| `disk-used`    | Number:DataAmount   | Currently used disk space (nodes and containers only).                                                       |
| `disk-total`   | Number:DataAmount   | Maximum available disk space.                                                                                |
| `uptime`       | Number:Time         | Time elapsed since the last boot.                                                                            |

> Note: Sending `OFF` to the `power` channel of a `node` shuts down the physical host. Use it with care.

## Full Example

`proxmox.things`:

```java
// Using an API token (recommended) with a self-signed certificate:
Bridge proxmox:host:pve "Proxmox Host" [ baseUrl="https://pve:8006/", apiTokenId="openhab@pam!mytoken", apiTokenSecret="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", trustAllCertificates=true, pollingInterval=30 ] {
    Thing node pve    "Proxmox Node pve"
    Thing vm   100    "Home Assistant VM"
    Thing lxc  101    "Pi-hole Container"
}
```

`proxmox.items`:

```java
Switch       Pve_Node_Power    "Node Power"          { channel="proxmox:node:pve:pve:power" }
Number:Dimensionless Pve_Node_Cpu "Node CPU [%.1f %%]" { channel="proxmox:node:pve:pve:cpu-load" }

Switch       Ha_Vm_Power       "Home Assistant"      { channel="proxmox:vm:pve:100:power" }
Number:DataAmount Ha_Vm_Mem    "Memory Used [%.1f %unit%]" { channel="proxmox:vm:pve:100:memory-used" }

Switch       Pihole_Power      "Pi-hole"             { channel="proxmox:lxc:pve:101:power" }
Number:Time  Pihole_Uptime     "Uptime [%.0f %unit%]" { channel="proxmox:lxc:pve:101:uptime" }
```
