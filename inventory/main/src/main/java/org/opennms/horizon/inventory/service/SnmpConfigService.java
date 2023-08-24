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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.mapper.SnmpConfigMapper;
import org.opennms.horizon.inventory.model.SnmpConfig;
import org.opennms.horizon.inventory.repository.SnmpConfigRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnmpConfigService {

    private final SnmpConfigRepository repository;

    private final SnmpConfigMapper snmpConfigMapper;

    public void saveOrUpdateSnmpConfig(String tenantId, Long locationId, String ipAddress, SnmpConfiguration snmpConfiguration) {
        var snmpConfig = new SnmpConfig();
        var inetAddress = InetAddressUtils.getInetAddress(ipAddress);
        var agentConfig = snmpConfigMapper.mapProtoToModel(snmpConfiguration);
        var existingConfig = repository.findByTenantIdAndLocationIdAndIpAddress(tenantId, locationId, inetAddress);
        if (existingConfig.isPresent()) {
            snmpConfig = existingConfig.get();
        } else {
            snmpConfig.setTenantId(tenantId);
            snmpConfig.setLocationId(locationId);
            snmpConfig.setIpAddress(inetAddress);
        }
        snmpConfig.setSnmpAgentConfig(agentConfig);
        repository.save(snmpConfig);
    }

    public Optional<SnmpConfiguration> getSnmpConfig(String tenantId, Long locationId, InetAddress ipAddress) {

        Optional<SnmpConfig> snmpConfig = repository.findByTenantIdAndLocationIdAndIpAddress(tenantId, locationId, ipAddress);
        if (snmpConfig.isPresent()) {
            var snmpConfiguration = snmpConfigMapper.mapModelToProto(snmpConfig.get().getSnmpAgentConfig());
            return Optional.of(snmpConfiguration);
        }
        return Optional.empty();
    }
}
