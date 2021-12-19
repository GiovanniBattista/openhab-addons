/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.octoprint.internal.handler;

import static org.openhab.binding.octoprint.internal.OctoPrintBindingConstants.CHANNEL_SYSTEM_COMMAND_PREFIX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.octoprint.internal.api.OctoPrintApi;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiCommunicationException;
import org.openhab.binding.octoprint.internal.api.exception.OctoPrintApiConfigurationException;
import org.openhab.binding.octoprint.internal.api.model.RegisteredSystemCommandsResponse;
import org.openhab.binding.octoprint.internal.api.model.SystemCommand;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.DefaultSystemChannelTypeProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Zupan
 */
public class OctoPrintSystemCommandHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OctoPrintSystemCommandHandler.class);

    private final OctoPrintApi api;

    public OctoPrintSystemCommandHandler(Thing thing, OctoPrintApi api) {
        super(thing);

        this.api = api;
    }

    @Override
    public void initialize() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelId = channelUID.getId();
        if (!channelId.startsWith(CHANNEL_SYSTEM_COMMAND_PREFIX)) {
            return;
        }

        try {
            Channel channel = getThing().getChannel(channelUID);
            if (channel != null) {
                Map<@NonNull String, @NonNull String> properties = channel.getProperties();
                String action = properties.get("action");
                String source = properties.get("source");

                if (command instanceof OnOffType && command == OnOffType.ON) {
                    api.executeSystemCommand(source, action);
                    updateState(channelUID, OnOffType.OFF);
                }
            }
        } catch (OctoPrintApiCommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, ex.getMessage());
        } catch (OctoPrintApiConfigurationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, ex.getMessage());
        } catch (Exception ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, ex.getMessage());
        }

    }

    public void createSystemCommandChannels(RegisteredSystemCommandsResponse response) {
        createSystemCommandChannels(response.getCore());
        createSystemCommandChannels(response.getCustom());

        updateSystemCommandChannelStates(response.getCore());
        updateSystemCommandChannelStates(response.getCustom());
    }

    private void createSystemCommandChannels(List<SystemCommand> systemCommands) {

        for (SystemCommand systemCommand : systemCommands) {
            createSystemCommandChannel(systemCommand);
        }
    }

    private void createSystemCommandChannel(SystemCommand systemCommand) {
        String channelId = generateSystemCommandChannelId(systemCommand);

        if (getThing().getChannel(channelId) != null) {
            return; // channel was already created, so we exit early here
        }

        logger.debug("Creating channel for system command: {}", channelId);
        ThingHandlerCallback callback = getCallback();
        if (callback != null) {
            Map<String, String> channelProperties = new HashMap<>();
            channelProperties.put("source", systemCommand.getSource());
            channelProperties.put("action", systemCommand.getAction());

            ChannelUID channelUID = new ChannelUID(getThing().getUID(), channelId);
            Channel channel = callback
                    .createChannelBuilder(channelUID, DefaultSystemChannelTypeProvider.SYSTEM_CHANNEL_TYPE_UID_POWER)
                    .withProperties(channelProperties).withLabel(systemCommand.getName()).build();

            Thing editedThing = editThing().withChannel(channel).build();
            updateThing(editedThing);
        }
    }

    private void updateSystemCommandChannelStates(List<SystemCommand> systemCommands) {
        for (SystemCommand systemCommand : systemCommands) {
            String channelId = generateSystemCommandChannelId(systemCommand);
            updateState(channelId, OnOffType.OFF);
        }
    }

    private String generateSystemCommandChannelId(SystemCommand systemCommand) {
        return CHANNEL_SYSTEM_COMMAND_PREFIX + systemCommand.getAction();
    }

}
