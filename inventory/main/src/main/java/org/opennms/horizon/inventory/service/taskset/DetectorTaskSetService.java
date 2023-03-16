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

import com.google.protobuf.Any;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.service.SnmpConfigService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForIpTask;

@Component
@RequiredArgsConstructor
public class DetectorTaskSetService {

    private static final Logger log = LoggerFactory.getLogger(DetectorTaskSetService.class);

    private final TaskSetPublisher taskSetPublisher;
    private final IpInterfaceRepository ipInterfaceRepository;
    private final SnmpConfigService snmpConfigService;

    private static final MonitorType[] DETECTOR_MONITOR_TYPES = {MonitorType.ICMP, MonitorType.SNMP};

    @Transactional
    public void sendDetectorTasks(Node node) {
        var tasks = getDetectorTasks(node);
        String tenantId = node.getTenantId();
        MonitoringLocation monitoringLocation = node.getMonitoringLocation();
        String location = monitoringLocation.getLocation();
        taskSetPublisher.publishNewTasks(tenantId, location, tasks);
    }

    public List<TaskDefinition> getDetectorTasks(Node node) {
        List<IpInterface> ipInterfaces =
            ipInterfaceRepository.findByNodeId(node.getId());
        if (ipInterfaces.isEmpty()) {
            return new ArrayList<>();
        }
        var optional = ipInterfaces.stream().filter(IpInterface::getSnmpPrimary).findFirst();
        var primaryInterface = optional.orElse(ipInterfaces.get(0));
        var ipAddress = primaryInterface.getIpAddress();
        var snmpConfiguration = snmpConfigService.getSnmpConfig(node.getTenantId(),
            node.getMonitoringLocation().getLocation(), ipAddress);
        List<TaskDefinition> tasks = new ArrayList<>();
        for (MonitorType monitorType : DETECTOR_MONITOR_TYPES) {
            if (monitorType.equals(MonitorType.SNMP)) {
                var list = addDetectorTasks(node, ipInterfaces, monitorType, snmpConfiguration.orElse(null));
                tasks.addAll(list);
            } else {
                var list = addDetectorTasks(node, ipInterfaces, monitorType);
                tasks.addAll(list);
            }
        }
        return tasks;
    }

    private List<TaskDefinition> addDetectorTasks(Node node, List<IpInterface> ipInterfaces, MonitorType monitorType,
                                                  SnmpConfiguration snmpConfiguration) {
        List<TaskDefinition> tasks = new ArrayList<>();
        for (IpInterface ipInterface : ipInterfaces) {
            var task = addDetectorTask(node.getId(), InetAddressUtils.toIpAddrString(ipInterface.getIpAddress()), monitorType, snmpConfiguration);

            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    private List<TaskDefinition> addDetectorTasks(Node node, List<IpInterface> ipInterfaces, MonitorType monitorType) {
        return addDetectorTasks(node, ipInterfaces, monitorType, null);
    }


    private TaskDefinition addDetectorTask(long nodeId, String ipAddress, MonitorType monitorType, SnmpConfiguration snmpConfiguration) {
        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String name = String.format("%s-detector", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sDetector", monitorTypeValue);
        TaskDefinition taskDefinition = null;
        Any configuration = null;

        switch (monitorType) {
            case ICMP -> configuration =
                Any.pack(IcmpDetectorRequest.newBuilder()
                    .setHost(ipAddress)
                    .setTimeout(TaskUtils.ICMP_DEFAULT_TIMEOUT_MS)
                    .setDscp(TaskUtils.ICMP_DEFAULT_DSCP)
                    .setAllowFragmentation(TaskUtils.ICMP_DEFAULT_ALLOW_FRAGMENTATION)
                    .setPacketSize(TaskUtils.ICMP_DEFAULT_PACKET_SIZE)
                    .setRetries(TaskUtils.ICMP_DEFAULT_RETRIES)
                    .build());
            case SNMP -> {
                var requestBuilder = SnmpDetectorRequest.newBuilder()
                    .setHost(ipAddress);
                if(snmpConfiguration != null) {
                    requestBuilder.setAgentConfig(snmpConfiguration);
                }
                configuration =
                    Any.pack(requestBuilder.build());
            }
            case UNRECOGNIZED -> log.warn("Unrecognized monitor type");
            case UNKNOWN -> log.warn("Unknown monitor type");
        }
        if (configuration != null) {
            String taskId = identityForIpTask(nodeId, ipAddress, name);
            TaskDefinition.Builder builder =
                TaskDefinition.newBuilder()
                    .setType(TaskType.DETECTOR)
                    .setPluginName(pluginName)
                    .setNodeId(nodeId)
                    .setId(taskId)
                    .setConfiguration(configuration);
            taskDefinition = builder.build();
        }
        return taskDefinition;
    }

}
