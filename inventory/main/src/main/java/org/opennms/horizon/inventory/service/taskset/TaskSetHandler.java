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
package org.opennms.horizon.inventory.service.taskset;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.service.SnmpConfigService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSetHandler {

    private final TaskSetPublisher taskSetPublisher;
    private final MonitorTaskSetService monitorTaskSetService;
    private final CollectorTaskSetService collectorTaskSetService;
    private final SnmpConfigService snmpConfigService;

    public void sendMonitorTask(
            Long locationId, MonitorType monitorType, IpInterface ipInterface, long nodeId, long monitoredServiceId) {
        String tenantId = ipInterface.getTenantId();
        // Currently, we only monitor interfaces that are discovered with ICMP ping
        // which are considered as primary interfaces for Snmp scan.
        if (ipInterface.getSnmpPrimary()) {
            var snmpConfig = snmpConfigService.getSnmpConfig(tenantId, locationId, ipInterface.getIpAddress());
            var task = monitorTaskSetService.getMonitorTask(
                    monitorType, ipInterface, nodeId, monitoredServiceId, snmpConfig.orElse(null));
            if (task != null) {
                taskSetPublisher.publishNewTasks(tenantId, locationId, Arrays.asList(task));
            }
        }
    }

    public void sendAzureMonitorTasks(AzureActiveDiscovery discovery, AzureScanItem item, long nodeId) {
        String tenantId = discovery.getTenantId();
        Long locationId = discovery.getLocationId();

        TaskDefinition task = monitorTaskSetService.addAzureMonitorTask(discovery, item, nodeId);
        taskSetPublisher.publishNewTasks(tenantId, locationId, Arrays.asList(task));
    }

    public void sendCollectorTask(Long locationId, MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String tenantId = ipInterface.getTenantId();
        // Collectors should only be invoked for primary interface
        if (monitorType.equals(MonitorType.SNMP) && ipInterface.getSnmpPrimary()) {
            var snmpConfig = snmpConfigService.getSnmpConfig(tenantId, locationId, ipInterface.getIpAddress());
            var task = collectorTaskSetService.addSnmpCollectorTask(ipInterface, nodeId, snmpConfig.orElse(null));
            if (task != null) {
                taskSetPublisher.publishNewTasks(tenantId, locationId, Arrays.asList(task));
            }
        }
    }

    public void sendAzureCollectorTasks(AzureActiveDiscovery discovery, AzureScanItem item, long nodeId) {
        String tenantId = discovery.getTenantId();
        Long locationId = discovery.getLocationId();

        TaskDefinition task = collectorTaskSetService.addAzureCollectorTask(discovery, item, nodeId);
        taskSetPublisher.publishNewTasks(tenantId, locationId, Arrays.asList(task));
    }
}
