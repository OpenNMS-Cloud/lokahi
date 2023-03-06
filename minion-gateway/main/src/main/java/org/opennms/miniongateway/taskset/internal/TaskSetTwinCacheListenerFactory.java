package org.opennms.miniongateway.taskset.internal;

import org.opennms.miniongateway.taskset.service.TaskSetStorageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.configuration.Factory;

/**
 * Factory that creates listeners for updates to TaskSets in the Ignite Cache and forwards the updates to Twin
 *  subscriptions.
 */
public class TaskSetTwinCacheListenerFactory implements Factory<TaskSetTwinCacheListener> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetTwinCacheListenerFactory.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final TaskSetStorageListener downstreamSession;

    public TaskSetTwinCacheListenerFactory(TaskSetStorageListener downstreamSession) {
        this.downstreamSession = downstreamSession;
    }

    @Override
    public TaskSetTwinCacheListener create() {
        LOG.debug("Creating listener for task set updates: tenant-id={}");
        return new TaskSetTwinCacheListener(downstreamSession);
    }
}
