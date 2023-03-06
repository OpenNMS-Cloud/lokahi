package org.opennms.miniongateway.taskset.internal;

import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.opennms.taskset.contract.TaskSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListenerException;
import java.util.Objects;

/**
 * Filter Ignite Cache Events for TaskSet updates to only those for the specific Tenant Location, identified by the
 *  Tenant ID + Location.
 */
public class TaskSetToTwinCacheEntryEventFilter implements CacheEntryEventFilter<TenantKey, TaskSet> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetToTwinCacheEntryEventFilter.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final String tenantId;
    private final String location;

    public TaskSetToTwinCacheEntryEventFilter(String tenantId, String location) {
        this.tenantId = tenantId;
        this.location = location;
    }

    @Override
    public boolean evaluate(CacheEntryEvent<? extends TenantKey, ? extends TaskSet> event) throws CacheEntryListenerException {
        TenantKey tenantKey = event.getKey();

        String eventTenantId = tenantKey.getTenantId();
        String eventLocation = tenantKey.getKey();

        //
        // Only process events that match the Tenant ID + Location (i.e. that share the same Task Set)
        //
        return (Objects.equals(tenantId, eventTenantId) && Objects.equals(location, eventLocation));
    }
}
