# Proxmox Binding

_Give some details about what this binding is meant for - a protocol, system, specific device._

_If possible, provide some resources like pictures, a YouTube video, etc. to give an impression of what can be done with this binding. You can place such resources into a `doc` folder next to this README.md._

## Supported Things

Node, VM, LXC

_Please describe the different supported things / devices within this section._
_Which different types are supported, which models were tested etc.?_
_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._


## Discovery

_Describe the available auto-discovery features here. Mention for what it works and what needs to be kept in mind when using it._

## Binding Configuration

_If your binding requires or supports general configuration settings, please create a folder ```cfg``` and place the configuration file ```<bindingId>.cfg``` inside it. In this section, you should link to this file and provide some information about the options. The file could e.g. look like:_

```
# Configuration for the Philips Hue Binding
#
# Default secret key for the pairing of the Philips Hue Bridge.
# It has to be between 10-40 (alphanumeric) characters
# This may be changed by the user for security reasons.
secret=openHABSecret
```

_Note that it is planned to generate some part of this based on the information that is available within ```src/main/resources/OH-INF/binding``` of your binding._

_If your binding does not offer any generic configurations, you can remove this section completely._


## Bridge Configuration

### Proxmox Configuration
The binding requires a Proxmox user having the following permissions:

```
Start/stop nodes: ["perm","/nodes/{node}",["Sys.PowerMgmt"]]
List VMs and LXCs: ["perm", "/vms/{vmid}", ["VM.Audit"]]
Start/stop vms: ["perm","/vms/{vmid}",["VM.PowerMgmt"]]
All other operations do not require special permissions
```
Therefore, it is best to create a separate user for the Proxmox binding. 
1. Open the Datacenter / Permissions / User and add a new Proxmox user (e.g. named Openhab).
    Note: For users in the Linux PAM realm, the user needs to be added locally via shell, see: https://pve.proxmox.com/wiki/User_Management
2. Go to Datacenter / Permsissions / Roles and create a new "Openhab" role with the permissions Sys.PowerMgmt, VM.Audit and VM.PowerMgmt
3. Navigate to Datacenter / Permissions and create a new permission for "/" and the openhab user with the above rule.

## Thing Configuration

_Describe what is needed to manually configure a thing, either through the (Paper) UI or via a thing-file. This should be mainly about its mandatory and optional configuration parameters. A short example entry for a thing file can help!_

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

## Channels

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

| channel  | type   | description                  |
|----------|--------|------------------------------|
| control  | Switch | This is the control channel  |

## Full Example

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap)._

## Any custom content here!

_Feel free to add additional sections for whatever you think should also be mentioned about your binding!_
