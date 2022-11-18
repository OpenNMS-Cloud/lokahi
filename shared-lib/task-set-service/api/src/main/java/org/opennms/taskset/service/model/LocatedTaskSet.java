package org.opennms.taskset.service.model;

import org.opennms.taskset.contract.TaskSet;

public class LocatedTaskSet {
    private final String tenantId;
    private final String location;
    private final TaskSet taskSet;

    public LocatedTaskSet(String tenantId, String location, TaskSet taskSet) {
        this.tenantId = tenantId;
        this.location = location;
        this.taskSet = taskSet;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getLocation() {
        return location;
    }

    public TaskSet getTaskSet() {
        return taskSet;
    }
}
