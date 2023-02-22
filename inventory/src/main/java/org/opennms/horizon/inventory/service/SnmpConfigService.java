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

import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.service.snmpconfig.SnmpConfigBean;
import org.opennms.horizon.shared.snmp.conf.xml.SnmpConfig;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SnmpConfigService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConfigurationService configurationService;

    public Configuration persistSnmpConfig(SnmpConfig snmpConfig, String tenantId, String location) {
        return configurationService.createSingle(mapSnmpConfigToConfigurationDTO(snmpConfig, tenantId, location));
    }

    private ConfigurationDTO mapSnmpConfigToConfigurationDTO(SnmpConfig config, String tenantId, String location) {
        return ConfigurationDTO.newBuilder()
            .setKey(ConfigKey.SNMP)
            .setTenantId(tenantId)
            .setValue(mapSnmpConfigToJson(config))
            .setLocation(location)
            .build();
    }

    private String mapSnmpConfigToJson(SnmpConfig config) {
        SnmpConfigBean snmpConfigBean = new SnmpConfigBean();
        snmpConfigBean.setTimeout(config.getTimeout());
        snmpConfigBean.setRetry(config.getRetry());
        snmpConfigBean.setReadCommunity(config.getReadCommunity());
        snmpConfigBean.setVersion(config.getVersion());
        try {
            return objectMapper.writeValueAsString(snmpConfigBean);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
