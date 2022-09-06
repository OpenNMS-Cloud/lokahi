package org.opennms.horizon.core.monitor.taskset;

import com.google.protobuf.Any;
import org.opennms.echo.contract.EchoRequest;
import org.opennms.snmp.contract.SnmpRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;

public class TaskSetManager {

    private MonitorTaskSetIdentityUtil monitorTaskSetIdentityUtil = new MonitorTaskSetIdentityUtil();

    private Map<String, TaskDefinition> tasksById = new TreeMap<>();

    public TaskSet getTaskSet() {
        return TaskSet.newBuilder()
            .addAllTaskDefinition(tasksById.values())
            .build();
    }

    public void addEchoTask(InetAddress inetAddress, String name, TaskType taskType, String pluginName, String schedule, EchoRequest echoRequest) {
        String taskId = monitorTaskSetIdentityUtil.identityForIpTask(inetAddress.getHostAddress(), name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setSchedule(schedule)
                .setId(taskId)
                .setConfiguration(Any.pack(echoRequest))
                ;

        TaskDefinition taskDefinition = builder.build();

        tasksById.put(taskId, taskDefinition);
    }

    public void addSnmpTask(InetAddress inetAddress, String name, TaskType taskType, String pluginName, String schedule, SnmpRequest snmpRequest) {
        String taskId = monitorTaskSetIdentityUtil.identityForIpTask(inetAddress.getHostAddress(), name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setSchedule(schedule)
                .setId(taskId)
                .setConfiguration(Any.pack(snmpRequest))
                ;

        TaskDefinition taskDefinition = builder.build();

        tasksById.put(taskId, taskDefinition);
    }
}
