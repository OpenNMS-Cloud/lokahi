/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigDTO;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigRequest;
import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.ConfigurationMapper;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.repository.ConfigurationRepository;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DiscoveryConfigService extends ConfigurationService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DiscoveryConfigDTO.Builder defaultBuilder = DiscoveryConfigDTO.newBuilder()
        .setRetries(1)
        .setTimeout(300L);

    public DiscoveryConfigService(ConfigurationRepository modelRepo, ConfigurationMapper mapper) {
        super(modelRepo, mapper);
    }

    public List<DiscoveryConfigDTO> createOrUpdateConfig(DiscoveryConfigRequest request, String tenantId) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isEmpty(request.getConfigName()) || StringUtils.isEmpty(request.getLocation()) || request.getIpAddressesList().isEmpty()) {
            throw new InventoryRuntimeException("Invalid config request: " + request);
        }
        return findByKey(tenantId, ConfigKey.DISCOVERY)
            .map(dbConfig -> {
                try {
                    DiscoveryConfigDTO discoveryConfig = requestToConfig(request);
                    ConfigurationDTO updatedConfig = mergeConfigValues(dbConfig, discoveryConfig);
                    Configuration newConfig = createOrUpdate(updatedConfig);
                    return jsonArrayToConfiglist((ArrayNode) newConfig.getValue());
                } catch (Exception e) {
                    log.error("Error while update config value: {} with request {}", dbConfig.getValue(), request);
                    throw new InventoryRuntimeException("Error while update config value", e);
                }
            }).orElseGet(() -> {
                DiscoveryConfigDTO discoveryConfig = requestToConfig(request);
                try {
                    ArrayNode arrayNode = objectMapper.createArrayNode();
                    arrayNode.add(objectMapper.readTree(ProtobufUtil.toJson(discoveryConfig)));
                    ConfigurationDTO newConfig = ConfigurationDTO.newBuilder()
                        .setValue(arrayNode.toString())
                        .setKey(ConfigKey.DISCOVERY)
                        .setLocation(request.getLocation())
                        .setTenantId(tenantId).build();
                    Configuration result = createOrUpdate(newConfig);
                    return jsonArrayToConfiglist((ArrayNode) result.getValue());
                } catch (Exception e) {
                    log.error("Error while creating new config with request {}", request);
                    throw new InventoryRuntimeException("Error while creating new config", e);
                }
            });
    }

    List<DiscoveryConfigDTO> jsonArrayToConfiglist(ArrayNode arrayNode) throws InvalidProtocolBufferException {
        List<DiscoveryConfigDTO> list = new ArrayList<>();
        for(JsonNode node: arrayNode) {
            list.add(ProtobufUtil.fromJson(node.toString(), DiscoveryConfigDTO.class));
        }
        return list;
    }

    private DiscoveryConfigDTO requestToConfig(DiscoveryConfigRequest request) {
        return DiscoveryConfigDTO.newBuilder(defaultBuilder.build())
            .setConfigName(request.getConfigName())
            .addAllIpAddresses(request.getIpAddressesList())
            .setSnmpConf(request.getSnmpConf()).build();
    }

    private ConfigurationDTO mergeConfigValues(ConfigurationDTO dbConfig, DiscoveryConfigDTO discoveryConfig) throws JsonProcessingException, InvalidProtocolBufferException {
        ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(dbConfig.getValue());
        arrayNode.add(objectMapper.readTree(ProtobufUtil.toJson(discoveryConfig)));
        return ConfigurationDTO.newBuilder(dbConfig)
            .setValue(arrayNode.toString()).build();
    }

    public Optional<DiscoveryConfigDTO> getDiscoveryConfigByName(String name, String tenantId) {
        return findByKey(tenantId, ConfigKey.DISCOVERY)
            .map(config -> {
                try {
                    return jsonArrayToConfiglist((ArrayNode) objectMapper.readTree(config.getValue()))
                        .stream().filter(c -> c.getConfigName().equals(name)).findFirst();
                } catch (Exception e) {
                    log.error("Error while get discovery config for name {}", name);
                    return Optional.<DiscoveryConfigDTO>empty();
                }
            })
            .orElse(null);
    }

    public List<DiscoveryConfigDTO> listDiscoveryConfigs(String tenantId) {

        return findByKey(tenantId, ConfigKey.DISCOVERY)
            .map(config -> {
                try {
                    return jsonArrayToConfiglist((ArrayNode) objectMapper.readTree(config.getValue()));
                } catch (Exception e) {
                    log.error("Error while list discovery config", e);
                    return new ArrayList<DiscoveryConfigDTO>();
                }
            }).orElseGet(ArrayList::new);
    }
}
