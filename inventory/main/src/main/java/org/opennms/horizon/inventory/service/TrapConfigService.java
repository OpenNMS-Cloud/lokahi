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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Any;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.service.taskset.TaskUtils;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.horizon.inventory.service.trapconfig.TrapConfigBean;
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

@Service
@RequiredArgsConstructor
public class TrapConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(TrapConfigService.class);
    public static final String TRAPS_CONFIG = "traps-config";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MonitoringLocationService monitoringLocationService;
    private final TaskSetPublisher taskSetPublisher;

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("trap-config-update-scheduler-%d")
            .build();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleConfigUpdate() {
        // This is work around for Minion not to have timeout for CloudToMinion Stream.
        // Keep sending Trap config every 15 mins.
        // https://opennms.atlassian.net/browse/LOK-2059
        executorService.scheduleAtFixedRate(this::sendTrapConfigToMinionAfterStartup, 900, 900, TimeUnit.SECONDS);
    }

    public void sendTrapConfigToMinionAfterStartup() {
        List<MonitoringLocationDTO> allLocations = monitoringLocationService.findAll();

        for (MonitoringLocationDTO dto : allLocations) {
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
        return config.getSnmpV3Users().stream()
                .map(snmpV3User -> {
                    return SnmpV3User.newBuilder()
                            .setEngineId(snmpV3User.getEngineId())
                            .setAuthPassphrase(snmpV3User.getAuthPassphrase())
                            .setAuthProtocol(snmpV3User.getAuthProtocol())
                            .setPrivacyPassphrase(snmpV3User.getPrivacyPassphrase())
                            .setPrivacyProtocol(snmpV3User.getPrivacyProtocol())
                            .build();
                })
                .collect(Collectors.toList());
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
