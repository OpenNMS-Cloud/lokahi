package org.opennms.horizon.inventory.service.taskset.manager;

import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultTaskSetManager implements TaskSetManager {

    private final Map<String, TaskSet> taskSetsByLocation = new HashMap<>();

    @Override
    public synchronized void addTaskSet(String location, TaskDefinition newTaskDefinition) {
        TaskSet existingTaskSet = taskSetsByLocation.get(location);
        TaskSet taskSet;
        if (existingTaskSet != null) {
            List<TaskDefinition> existingTaskDefinitions = new ArrayList<>(existingTaskSet.getTaskDefinitionList());
            existingTaskDefinitions.removeIf(task -> task.getId().equals(newTaskDefinition.getId()));
            taskSet = TaskSet.newBuilder().addAllTaskDefinition(existingTaskDefinitions)
                .addTaskDefinition(newTaskDefinition).build();
        } else {
            taskSet = TaskSet.newBuilder().addTaskDefinition(newTaskDefinition).build();
        }
        taskSetsByLocation.put(location, taskSet);
    }

    @Override
    public synchronized TaskSet getTaskSet(String location) {
        return taskSetsByLocation.get(location);
    }
}
