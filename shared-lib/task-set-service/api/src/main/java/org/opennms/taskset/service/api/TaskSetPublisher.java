package org.opennms.taskset.service.api;

import org.opennms.taskset.contract.TaskSet;

public interface TaskSetPublisher {
    String TASK_SET_PUBLISH_SERVICE = "task-set.pub-task";

    void publishTaskSet(String location, TaskSet taskSet);
}
