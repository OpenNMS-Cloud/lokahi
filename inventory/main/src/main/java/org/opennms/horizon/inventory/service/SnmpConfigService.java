/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.service;

import java.net.InetAddress;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.mapper.SnmpConfigMapper;
import org.opennms.horizon.inventory.model.SnmpConfig;
import org.opennms.horizon.inventory.repository.SnmpConfigRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnmpConfigService {

    private final SnmpConfigRepository repository;

    private final SnmpConfigMapper snmpConfigMapper;

    public void saveOrUpdateSnmpConfig(
            String tenantId, Long locationId, String ipAddress, SnmpConfiguration snmpConfiguration) {
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

        Optional<SnmpConfig> snmpConfig =
                repository.findByTenantIdAndLocationIdAndIpAddress(tenantId, locationId, ipAddress);
        if (snmpConfig.isPresent()) {
            var snmpConfiguration =
                    snmpConfigMapper.mapModelToProto(snmpConfig.get().getSnmpAgentConfig());
            return Optional.of(snmpConfiguration);
        }
        return Optional.empty();
    }
}
