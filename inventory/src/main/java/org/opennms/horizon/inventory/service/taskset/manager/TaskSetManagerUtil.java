package org.opennms.horizon.inventory.service.taskset.manager;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.service.taskset.identity.TaskSetIdentityUtil;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TaskSetManagerUtil {
    private final TaskSetManager taskSetManager;
    private final TaskSetIdentityUtil taskSetIdentityUtil;

    public void addTask(String location, String ipAddress, String name, TaskType taskType, String pluginName) {

        String taskId = taskSetIdentityUtil.identityForIpTask(ipAddress, name);
        addTaskToTaskSet(location, taskType, pluginName, null, null, taskId);
    }

    public void addTask(String location, String ipAddress, String name, TaskType taskType,
                        String pluginName, String schedule, Any configuration) {

        String taskId = taskSetIdentityUtil.identityForIpTask(ipAddress, name);
        addTaskToTaskSet(location, taskType, pluginName, schedule, configuration, taskId);
    }

    public void addTask(String location, String ipAddress, String name, TaskType taskType,
                        String pluginName, Any configuration) {

        String taskId = taskSetIdentityUtil.identityForIpTask(ipAddress, name);
        addTaskToTaskSet(location, taskType, pluginName, null, configuration, taskId);
    }

    private void addTaskToTaskSet(String location, TaskType taskType, String pluginName, String schedule,
                                  Any configuration, String taskId) {

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setId(taskId);

        if (StringUtils.isNotBlank(schedule)) {
            builder.setSchedule(schedule);
        }

        if (!Objects.isNull(configuration)) {
            builder.setConfiguration(configuration);
        }

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(location, taskDefinition);
    }
}
