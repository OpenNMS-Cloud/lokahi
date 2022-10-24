package org.opennms.taskset.service.model;

import org.opennms.taskset.contract.TaskSet;

public class LocatedTaskSet {
    private final String location;
    private final TaskSet taskSet;

    public LocatedTaskSet(String location, TaskSet taskSet) {
        this.location = location;
        this.taskSet = taskSet;
    }

    public String getLocation() {
        return location;
    }

    public TaskSet getTaskSet() {
        return taskSet;
    }
}
