/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.config.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.opennms.horizon.config.service.api.ConfigService;
import org.opennms.horizon.db.kvstore.api.JsonStore;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.opennms.horizon.config.service.api.ConfigConstants.CONFIG;
import static org.opennms.horizon.config.service.api.ConfigConstants.CONFIG_NAMES;

public class DefaultConfigService implements ConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultConfigService.class);
    private final JsonStore jsonStore;
    private final EventForwarder eventForwarder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultConfigService(JsonStore jsonStore, EventForwarder eventForwarder) {
        this.jsonStore = jsonStore;
        this.eventForwarder = eventForwarder;
    }


    @Override
    public void addConfig(String configName, String jsonConfig, String source) {
        addConfigNameToJsonStore(configName);
        try {
            Optional<String> existingConfig = getConfig(configName);
            if (existingConfig.isPresent() && existingConfig.get().equals(jsonConfig)) {
                LOG.info("Given config matches with existing config for {}, no update made", configName);
            }
            objectMapper.readTree(jsonConfig);
        } catch (JsonProcessingException e) {
            LOG.error("Exception while parsing config for {}", configName, e);
            throw new IllegalArgumentException("Invalid json " + jsonConfig);
        }
        jsonStore.put(configName, jsonConfig, CONFIG);
        sendConfigUpdatedEvent(configName, source);
    }

    @Override
    public void updateConfig(String configName, String jsonConfig, String source) {
        // May change later, currently this is identical to addConfig
         addConfig(configName, jsonConfig, source);
    }

    @Override
    public Optional<String> getConfig(String configName) {
        return jsonStore.get(configName, CONFIG);
    }

    @Override
    public List<String> getConfigNames() {
        Optional<String> configNamesJson = jsonStore.get(CONFIG_NAMES, CONFIG);
        if (configNamesJson.isPresent()) {
            var configNamesArray = new JSONArray(configNamesJson.get());
            return configNamesArray.toList().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void addConfigNameToJsonStore(String configName) {
        List<String> configNameList = getConfigNames();
        if (!configNameList.contains(configName)) {
            configNameList.add(configName);
            jsonStore.put(CONFIG_NAMES, new JSONArray(configNameList).toString(), CONFIG);
        }
    }

    private void sendConfigUpdatedEvent(String configName, String source) {
        EventBuilder eventBuilder = new EventBuilder(EventConstants.CONFIG_UPDATED_UEI, source);
        eventBuilder.addParam(EventConstants.PARM_CONFIG_NAME, configName);
        eventForwarder.sendNowSync(eventBuilder.getEvent());
    }
}
