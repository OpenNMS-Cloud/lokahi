package org.opennms.horizon.core.monitor.taskset;

import com.google.protobuf.Any;
import org.opennms.horizon.taskset.manager.TaskSetManager;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;

public class TaskSetManagerUtil {

    private MonitorTaskSetIdentityUtil monitorTaskSetIdentityUtil = new MonitorTaskSetIdentityUtil();

    private TaskSetManager taskSetManager;

    public TaskSetManagerUtil(TaskSetManager taskSetManager) {
        this.taskSetManager = taskSetManager;
    }

    public void addEchotask(String location, InetAddress inetAddress, String name, TaskType taskType, String pluginName, String schedule, IcmpMonitorRequest echoRequest) {
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

        taskSetManager.addTaskSet(location, taskDefinition);
    }

    public void addSnmpTask(String location, InetAddress inetAddress, String name, TaskType taskType, String pluginName, String schedule, SnmpMonitorRequest snmpMonitorRequest) {
        String taskId = monitorTaskSetIdentityUtil.identityForIpTask(inetAddress.getHostAddress(), name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setSchedule(schedule)
                .setId(taskId)
                .setConfiguration(Any.pack(snmpMonitorRequest))
                ;

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(location, taskDefinition);
    }

    public void addSnmpTask(String location, InetAddress inetAddress, String name, TaskType taskType, String pluginName, SnmpDetectorRequest snmpDetectorRequest) {
        String taskId = monitorTaskSetIdentityUtil.identityForIpTask(inetAddress.getHostAddress(), name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setId(taskId)
                .setConfiguration(Any.pack(snmpDetectorRequest))
            ;

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(location, taskDefinition);
    }
}
