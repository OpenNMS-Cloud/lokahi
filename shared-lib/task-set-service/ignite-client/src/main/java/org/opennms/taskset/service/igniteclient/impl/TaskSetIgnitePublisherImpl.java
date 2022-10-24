package org.opennms.taskset.service.igniteclient.impl;

import org.apache.ignite.client.IgniteClient;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.model.LocatedTaskSet;

import java.util.function.Consumer;

public class TaskSetIgnitePublisherImpl implements TaskSetPublisher {

    private IgniteClient igniteClient;

//========================================
// Getters and Setters
//----------------------------------------

    public IgniteClient getIgniteClient() {
        return igniteClient;
    }

    public void setIgniteClient(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
    }


//========================================
// Ignite Task Set Client API
//----------------------------------------

    @Override
    public void publishTaskSet(String location, TaskSet taskSet) {
        LocatedTaskSet locatedTaskSet = new LocatedTaskSet(location, taskSet);

        igniteClient.services().serviceProxy(TASK_SET_PUBLISH_SERVICE, Consumer.class).accept(locatedTaskSet);
    }
}
