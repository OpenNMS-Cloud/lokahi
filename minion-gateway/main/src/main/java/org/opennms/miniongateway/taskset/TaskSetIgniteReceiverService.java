package org.opennms.miniongateway.taskset;

import org.apache.ignite.Ignite;
import org.apache.ignite.resources.SpringResource;
import org.apache.ignite.services.Service;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.model.LocatedTaskSet;

import java.util.function.Consumer;

public class TaskSetIgniteReceiverService implements Consumer<LocatedTaskSet>, Service {
    @SpringResource(resourceName = "ignite")
    private final transient Ignite ignite;

    @SpringResource(resourceName = "taskSetPublisher")
    private final transient TaskSetPublisher taskSetPublisher;

    private boolean shutdownInd = false;

    public TaskSetIgniteReceiverService(Ignite ignite, TaskSetPublisher taskSetPublisher) {
        this.ignite = ignite;
        this.taskSetPublisher = taskSetPublisher;
    }

//========================================
// Internals
//----------------------------------------

    @Override
    public void accept(LocatedTaskSet locatedTaskSet) {
        taskSetPublisher.publishTaskSet(locatedTaskSet.getLocation(), locatedTaskSet.getTaskSet());
    }
}
