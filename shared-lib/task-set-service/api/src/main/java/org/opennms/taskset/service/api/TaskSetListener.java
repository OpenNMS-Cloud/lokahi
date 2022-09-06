package org.opennms.taskset.service.api;

import org.opennms.taskset.contract.TaskSet;

public interface TaskSetListener {
    void onTaskSetUpdate(TaskSet taskSet);
}
