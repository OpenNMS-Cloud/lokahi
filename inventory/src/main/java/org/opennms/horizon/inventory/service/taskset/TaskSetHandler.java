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

package org.opennms.horizon.inventory.service.taskset;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.taskset.api.TaskSetPublisher;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSetHandler {

    private final TaskSetPublisher taskSetPublisher;
    private final MonitorTaskSetService monitorTaskSetService;
    private final CollectorTaskSetService collectorTaskSetService;

    public void sendMonitorTask(String location, MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String tenantId = ipInterface.getTenantId();

        var task = monitorTaskSetService.getMonitorTask(monitorType, ipInterface, nodeId);
        if (task != null) {
            taskSetPublisher.publishNewTasks(tenantId, location, Arrays.asList(task));
        }
    }

    public void sendAzureMonitorTasks(AzureCredential credential, AzureScanItem item, String ipAddress, long nodeId) {
        String tenantId = credential.getTenantId();
        String location = credential.getMonitoringLocation().getLocation();

        TaskDefinition task = monitorTaskSetService.addAzureMonitorTask(credential, item, ipAddress, nodeId);
        taskSetPublisher.publishNewTasks(tenantId, location, Arrays.asList(task));
    }

    public void sendCollectorTask(String location, MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String tenantId = ipInterface.getTenantId();
        var task = collectorTaskSetService.getCollectorTask(monitorType, ipInterface, nodeId);
        if (task != null) {
            taskSetPublisher.publishNewTasks(tenantId, location, Arrays.asList(task));
        }
    }

    public void sendAzureCollectorTasks(AzureCredential credential, AzureScanItem item, String ipAddress, long nodeId) {
        String tenantId = credential.getTenantId();
        String location = credential.getMonitoringLocation().getLocation();

        TaskDefinition task = collectorTaskSetService.addAzureCollectorTask(credential, item, ipAddress, nodeId);
        taskSetPublisher.publishNewTasks(tenantId, location, Arrays.asList(task));
    }

}
