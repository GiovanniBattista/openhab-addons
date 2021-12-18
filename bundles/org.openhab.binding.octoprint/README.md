# OctoPrint Binding

This binding fetches data from an existing OctoPrint instance. 

## Supported Things

The binding supports the following thing:

| Thing type  | Name                   
|-------------|------------------------
| octoprint   | The octoprint instance 

## Discovery

All OctoPrint instances should be discovered automatically. 

## Thing Configuration

| Parameter       | Description                                                                                         | Mandatory 
|-----------------|-----------------------------------------------------------------------------------------------------| ----------
| hostname        | The hostname or IP address of the OctoPrint instance. Will be set automatically during discovery.   | Yes
| port            | The port of the OctoPrint instance. Will be set automatically during discovery.                     | Yes
| user            | The OctoPrint user. This value has to be set once manually.                                         | Yes
| path            | The base path of the OctoPrint instance. Will be set automatically during discovery.                | No
| apiKey          | The thing tries to retrieve the API key automatically from the OctoPrint instance. The precondition for this is that the Application Keys Plugin which comes bundled with OctoPrint is available and enabled. Otherwise the user has to create an API key manually through the OctoPrint Web interface and set it here.  | Yes
| refreshInterval | The refresh interval for polling the OctoPrint instance.                                            | No


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
