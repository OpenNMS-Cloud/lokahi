package org.opennms.horizon.inventory.service.taskset.manager;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.service.taskset.identity.TaskSetIdentityUtil;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskSetManagerUtil {
    private final TaskSetManager taskSetManager;
    private final TaskSetIdentityUtil taskSetIdentityUtil;

//    todo: add icmp detector task request
    public void addIcmpTask(String location, String ipAddress, String name, TaskType taskType, String pluginName) {

        String taskId = taskSetIdentityUtil.identityForIpTask(ipAddress, name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setId(taskId);

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(location, taskDefinition);
    }

    public void addSnmpTask(String location, String ipAddress, String name, TaskType taskType,
                            String pluginName, SnmpDetectorRequest snmpDetectorRequest) {

        String taskId = taskSetIdentityUtil.identityForIpTask(ipAddress, name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setId(taskId)
                .setConfiguration(Any.pack(snmpDetectorRequest));

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(location, taskDefinition);
    }
}
