package org.opennms.horizon.inventory.service.taskset;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManager;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManagerUtil;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitorTaskSetService {
    private static final Logger log = LoggerFactory.getLogger(MonitorTaskSetService.class);
    private static final String DEFAULT_SCHEDULE = "5000";
    private final TaskSetManagerUtil taskSetManagerUtil;
    private final TaskSetManager taskSetManager;
    private final TaskSetPublisher taskSetPublisher;

    public void sendMonitorTask(String location, MonitorType monitorType, IpInterface ipInterface) {
        addMonitorTask(location, monitorType, ipInterface);
        sendTaskSet(location);
    }

    private void addMonitorTask(String location, MonitorType monitorType, IpInterface ipInterface) {
        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String ipAddress = ipInterface.getIpAddress().getAddress();

        String name = String.format("%s-monitor", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sMonitor", monitorTypeValue);

        switch (monitorType) {
            case ICMP: {
                //todo: add request
                taskSetManagerUtil.addIcmpTask(location, ipAddress, name, TaskType.MONITOR, pluginName);
                break;
            }
            case SNMP: {
                SnmpMonitorRequest request =
                    SnmpMonitorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setTimeout(18000)
                        .setRetries(2)
                        .build();

                taskSetManagerUtil.addSnmpTask(location, ipAddress, name, TaskType.MONITOR, pluginName, DEFAULT_SCHEDULE, request);
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

    private void sendTaskSet(String location) {
        TaskSet taskSet = taskSetManager.getTaskSet(location);
        taskSetPublisher.publishTaskSet(location, taskSet);
    }
}
