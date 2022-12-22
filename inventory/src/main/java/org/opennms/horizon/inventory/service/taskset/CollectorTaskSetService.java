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

package org.opennms.horizon.inventory.service.taskset;

import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManager;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManagerUtil;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.horizon.snmp.api.Version;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskType;
import org.springframework.stereotype.Component;

import com.google.protobuf.Any;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class CollectorTaskSetService {
    private final TaskSetManagerUtil taskSetManagerUtil;
    private final TaskSetManager taskSetManager;

    public void sendCollectorTask(String location, MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String tenantId = ipInterface.getTenantId();
        addCollectorTask(location, monitorType, ipInterface, nodeId);
        taskSetManager.sendTaskSet(tenantId, location);
    }

    private void addCollectorTask(String location, MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String ipAddress = ipInterface.getIpAddress().getAddress();
        String tenantId = ipInterface.getTenantId();

        String name = String.format("%s-collector", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sCollector", monitorTypeValue);

        if (monitorType == MonitorType.SNMP) {
            Any configuration =
                Any.pack(SnmpCollectorRequest.newBuilder()
                    .setHost(ipAddress)
                    .setAgentConfig(SnmpConfiguration.newBuilder()
                        .setAddress(ipAddress)
                        .setVersion(Version.v2)
                        .setTimeout(30000).build())
                    .setNodeId(nodeId)
                    .build());

            taskSetManagerUtil.addTask(tenantId, location, ipAddress, name, TaskType.COLLECTOR, pluginName,
                Constants.DEFAULT_SCHEDULE, nodeId, configuration);
        }
    }
}
