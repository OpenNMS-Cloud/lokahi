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
package org.opennms.horizon.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.service.taskset.TaskUtils;
import org.opennms.horizon.inventory.service.trapconfig.TrapConfigBean;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.sink.traps.contract.ListenerConfig;
import org.opennms.sink.traps.contract.SnmpV3User;
import org.opennms.sink.traps.contract.TrapConfig;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrapConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(TrapConfigService.class);
    public final static String TRAPS_CONFIG  = "traps-config";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MonitoringLocationService monitoringLocationService;
    private final TaskSetPublisher taskSetPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void sendTrapConfigToMinionAfterStartup() {
        List<MonitoringLocationDTO> allLocations = monitoringLocationService.findAll();

        for(MonitoringLocationDTO dto : allLocations) {
            sendTrapConfigToMinion(dto.getTenantId(), dto.getId());
        }
    }

    public void sendTrapConfigToMinion(String tenantId, Long locationId) {
        TrapConfigBean trapConfigBean = readTrapConfig();
        TrapConfig trapConfig = mapBeanToProto(trapConfigBean);
        publishTrapConfig(tenantId, locationId, trapConfig);
    }

    private TrapConfig mapBeanToProto(TrapConfigBean config) {
        return TrapConfig.newBuilder()
            .setSnmpTrapAddress(config.getSnmpTrapAddress())
            .setSnmpTrapPort(config.getSnmpTrapPort())
            .setNewSuspectOnTrap(config.getNewSuspectOnTrap())
            .setIncludeRawMessage(config.isIncludeRawMessage())
//            .setUseAddressFromVarbind(config.shouldUseAddressFromVarbind())
            .setListenerConfig(ListenerConfig.newBuilder()
                .setBatchIntervalMs(config.getBatchIntervalMs())
                .setBatchSize(config.getBatchSize())
                .setQueueSize(config.getQueueSize())
                .setNumThreads(config.getNumThreads()))
            .addAllSnmpV3User(mapSnmpV3Users(config))
            .build();
    }

    private List<SnmpV3User> mapSnmpV3Users(TrapConfigBean config) {
        return config.getSnmpV3Users().stream().map(snmpV3User -> {
            return SnmpV3User.newBuilder()
                .setEngineId(snmpV3User.getEngineId())
                .setAuthPassphrase(snmpV3User.getAuthPassphrase())
                .setAuthProtocol(snmpV3User.getAuthProtocol())
                .setPrivacyPassphrase(snmpV3User.getPrivacyPassphrase())
                .setPrivacyProtocol(snmpV3User.getPrivacyProtocol())
                .build();
        }).collect(Collectors.toList());
    }

    private void publishTrapConfig(String tenantId, Long locationId, TrapConfig trapConfig) {
        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
            .setId(TaskUtils.identityForConfig(TRAPS_CONFIG, locationId))
            .setPluginName("trapd.listener.config")
            .setType(TaskType.LISTENER)
            .setConfiguration(Any.pack(trapConfig))
            .build();
        var taskList = new ArrayList<TaskDefinition>();
        taskList.add(taskDefinition);

        taskSetPublisher.publishNewTasks(tenantId, locationId, taskList);
    }

    private TrapConfigBean readTrapConfig() {
        try {
            URL url = this.getClass().getResource("/trapd-config.json");
            return objectMapper.readValue(url, TrapConfigBean.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
