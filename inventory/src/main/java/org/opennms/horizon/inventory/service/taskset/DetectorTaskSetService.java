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

import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManager;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManagerUtil;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.protobuf.Any;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class DetectorTaskSetService {
    private static final Logger log = LoggerFactory.getLogger(DetectorTaskSetService.class);
    private final TaskSetManagerUtil taskSetManagerUtil;
    private final IpInterfaceRepository ipInterfaceRepository;
    private final TaskSetManager taskSetManager;

    private static final MonitorType[] DETECTOR_MONITOR_TYPES = {MonitorType.ICMP, MonitorType.SNMP};

    @Transactional
    public void sendDetectorTasks(Node node) {

        List<IpInterface> ipInterfaces =
            ipInterfaceRepository.findByNodeId(node.getId());

        for (MonitorType monitorType : DETECTOR_MONITOR_TYPES) {
            addDetectorTasks(node, ipInterfaces, monitorType);
        }
        sendTaskSet(node);
    }

    public void sendDetectorTaskForNodes(Map<String, Map<String, List<NodeDTO>>> nodeListMap) {
        for(MonitorType monitorType: DETECTOR_MONITOR_TYPES) {
            nodeListMap.forEach((tenantId, locationNodes) -> locationNodes.forEach((location, nodes)->
                nodes.forEach(node -> node.getIpInterfacesList().forEach(ip ->
                    addDetectorTask(node.getId(), tenantId, ip.getIpAddress(), location, monitorType)))));
        }
        nodeListMap.forEach((tenantId, locationMap) -> locationMap.forEach((location, node) -> taskSetManager.sendTaskSet(tenantId, location)));
    }

    private void addDetectorTasks(Node node, List<IpInterface> ipInterfaces, MonitorType monitorType) {
        for (IpInterface ipInterface : ipInterfaces) {
            addDetectorTask(node.getId(), node.getTenantId(), ipInterface.getIpAddress().getAddress(),
                node.getMonitoringLocation().getLocation(), monitorType);
        }
    }

    private void addDetectorTask(long nodeId, String tenantId, String ipAddress, String location, MonitorType monitorType) {
        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String name = String.format("%s-detector", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sDetector", monitorTypeValue);

        switch (monitorType) {
            case ICMP: {
                Any configuration =
                    Any.pack(IcmpDetectorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setTimeout(Constants.Icmp.DEFAULT_TIMEOUT)
                        .setDscp(Constants.Icmp.DEFAULT_DSCP)
                        .setAllowFragmentation(Constants.Icmp.DEFAULT_ALLOW_FRAGMENTATION)
                        .setPacketSize(Constants.Icmp.DEFAULT_PACKET_SIZE)
                        .setRetries(Constants.Icmp.DEFAULT_RETRIES)
                        .build());

                taskSetManagerUtil.addTask(tenantId, location, ipAddress, name,
                    TaskType.DETECTOR, pluginName, configuration, nodeId);
                break;
            }
            case SNMP: {
                Any configuration =
                    Any.pack(SnmpDetectorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setTimeout(Constants.Snmp.DEFAULT_TIMEOUT)
                        .setRetries(Constants.Snmp.DEFAULT_RETRIES)
                        .build());

                taskSetManagerUtil.addTask(tenantId, location, ipAddress, name,
                    TaskType.DETECTOR, pluginName, configuration, nodeId);
                break;
            }
            case UNRECOGNIZED: {
                log.warn("Unrecognized monitor type");
                break;
            }
            case UNKNOWN: {
                log.warn("Unknown monitor type");
                break;
            }
        }
    }

    private void sendTaskSet(Node node) {
        String tenantId = node.getTenantId();
        MonitoringLocation monitoringLocation = node.getMonitoringLocation();
        String location = monitoringLocation.getLocation();
        taskSetManager.sendTaskSet(tenantId, location);
    }
}
