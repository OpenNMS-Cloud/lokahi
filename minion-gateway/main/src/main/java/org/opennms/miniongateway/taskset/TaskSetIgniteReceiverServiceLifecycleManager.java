package org.opennms.miniongateway.taskset;

import org.apache.ignite.Ignite;
import org.opennms.taskset.service.api.TaskSetPublisher;

public class TaskSetIgniteReceiverServiceLifecycleManager {
    private final Ignite ignite;
    private final TaskSetIgniteReceiverService taskSetIgniteReceiverService;

    public TaskSetIgniteReceiverServiceLifecycleManager(Ignite ignite, TaskSetIgniteReceiverService taskSetIgniteReceiverService) {
        this.ignite = ignite;
        this.taskSetIgniteReceiverService = taskSetIgniteReceiverService;
    }

    public void start() {
        ignite.services().deployNodeSingleton(TaskSetPublisher.TASK_SET_PUBLISH_SERVICE, taskSetIgniteReceiverService);
    }
}
