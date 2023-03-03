package org.opennms.miniongateway.taskset.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.configuration.Factory;

/**
 *
 */
public class TaskSetToTwinCacheEntryEventFilterFactory implements Factory<TaskSetToTwinCacheEntryEventFilter> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetToTwinCacheEntryEventFilterFactory.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final String tenantId;
    private final String location;

    public TaskSetToTwinCacheEntryEventFilterFactory(String tenantId, String location) {
        this.tenantId = tenantId;
        this.location = location;
    }

    @Override
    public TaskSetToTwinCacheEntryEventFilter create() {
        return new TaskSetToTwinCacheEntryEventFilter(tenantId, location);
    }
}

