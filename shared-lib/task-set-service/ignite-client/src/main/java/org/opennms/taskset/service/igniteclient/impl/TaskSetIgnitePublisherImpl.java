package org.opennms.taskset.service.igniteclient.impl;

import org.apache.ignite.Ignite;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.model.LocatedTaskSet;

public class TaskSetIgnitePublisherImpl implements TaskSetPublisher {
    private Ignite ignite;

//========================================
// Getters and Setters
//----------------------------------------

    public Ignite getIgnite() {
        return ignite;
    }

    public void setIgnite(Ignite ignite) {
        this.ignite = ignite;
    }


//========================================
// Ignite Task Set Client API
//----------------------------------------

    @Override
    public void publishTaskSet(String location, TaskSet taskSet) {
        LocatedTaskSet locatedTaskSet = new LocatedTaskSet(location, taskSet);

        ignite.message().send(TaskSetPublisher.TASK_SET_TOPIC, locatedTaskSet);
    }
}
