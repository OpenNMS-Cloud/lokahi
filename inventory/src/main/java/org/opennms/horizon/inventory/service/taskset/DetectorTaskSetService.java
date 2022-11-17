package org.opennms.horizon.inventory.service.taskset;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManager;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManagerUtil;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DetectorTaskSetService {
    private static final Logger log = LoggerFactory.getLogger(DetectorTaskSetService.class);
    private static final int DEFAULT_SNMP_TIMEOUT = 18000;
    private static final int DEFAULT_SNMP_RETRIES = 2;
    private final TaskSetManagerUtil taskSetManagerUtil;
    private final TaskSetManager taskSetManager;
    private final TaskSetPublisher taskSetPublisher;
    private final IpInterfaceRepository ipInterfaceRepository;

    //todo: add ICMP
    private static final MonitorType[] DETECTOR_MONITOR_TYPES = {MonitorType.SNMP};

    public void sendDetectorTasks(Node node) {

        List<IpInterface> ipInterfaces = ipInterfaceRepository
            .findByNodeIdAndTenantId(node.getId(), node.getTenantId());

        for (MonitorType monitorType : DETECTOR_MONITOR_TYPES) {
            addDetectorTasks(node, ipInterfaces, monitorType);
        }

        sendTaskSet(node);
    }

    private void addDetectorTasks(Node node, List<IpInterface> ipInterfaces, MonitorType monitorType) {
        for (IpInterface ipInterface : ipInterfaces) {
            addDetectorTask(node, ipInterface, monitorType);
        }
    }

    private void addDetectorTask(Node node, IpInterface ipInterface, MonitorType monitorType) {
        String ipAddress = ipInterface.getIpAddress().getAddress();
        MonitoringLocation monitoringLocation = node.getMonitoringLocation();
        String location = monitoringLocation.getLocation();

        String monitorTypeValue = monitorType.getValueDescriptor().getName();

        String name = String.format("%s-detector", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sDetector", monitorTypeValue);

        switch (monitorType) {
            case ICMP: {
                //todo: add request
                taskSetManagerUtil.addTask(location, ipAddress, name, TaskType.DETECTOR, pluginName);
                break;
            }
            case SNMP: {
                Any configuration =
                    Any.pack(SnmpDetectorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setTimeout(DEFAULT_SNMP_TIMEOUT)
                        .setRetries(DEFAULT_SNMP_RETRIES)
                        .build());

                taskSetManagerUtil.addTask(location, ipAddress, name, TaskType.DETECTOR, pluginName, configuration);
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
        MonitoringLocation monitoringLocation = node.getMonitoringLocation();
        String location = monitoringLocation.getLocation();
        TaskSet taskSet = taskSetManager.getTaskSet(location);

        taskSetPublisher.publishTaskSet(location, taskSet);
    }
}
