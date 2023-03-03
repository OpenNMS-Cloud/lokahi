package org.opennms.miniongateway.taskset.service;

import org.opennms.taskset.contract.TaskSet;

import java.io.IOException;

public interface TaskSetStorageListener {
    void publish(String tenantId, String location, TaskSet taskSet) throws IOException;
}
