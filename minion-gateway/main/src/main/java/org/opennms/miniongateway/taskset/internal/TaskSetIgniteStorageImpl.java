package org.opennms.miniongateway.taskset.internal;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.opennms.miniongateway.taskset.service.TaskSetStorage;
import org.opennms.miniongateway.taskset.service.TaskSetStorageListener;
import org.opennms.taskset.contract.TaskSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import java.util.IdentityHashMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

/**
 *
 */
@Component
public class TaskSetIgniteStorageImpl implements TaskSetStorage {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetIgniteStorageImpl.class);

    private Logger LOG = DEFAULT_LOGGER;

    public static final String TASK_SET_IGNITE_CACHE_NAME = "taskSetCache";

    private final IgniteCache<TenantKey, TaskSet> taskSetIgniteCache;

    private final IdentityHashMap<TaskSetStorageListener, MutableCacheEntryListenerConfiguration<TenantKey, TaskSet>>
        cacheListenerConfigForPublisherSession = new IdentityHashMap<>();

    private final Object lock = new Object();

    public TaskSetIgniteStorageImpl(@Autowired Ignite ignite) {
        taskSetIgniteCache = ignite.getOrCreateCache(TASK_SET_IGNITE_CACHE_NAME);
    }

    @Override
    public TaskSet getTaskSetForLocation(String tenantId, String location) {
        TenantKey tenantKey = new TenantKey(tenantId, location);

        return taskSetIgniteCache.get(tenantKey);
    }

    @Override
    public void putTaskSetForLocation(String tenantId, String location, TaskSet taskSet) {
        TenantKey tenantKey = new TenantKey(tenantId, location);

        taskSetIgniteCache.put(tenantKey, taskSet);
    }

    @Override
    public boolean deleteTaskSetForLocation(String tenantId, String location) {
        TenantKey tenantKey = new TenantKey(tenantId, location);

        return taskSetIgniteCache.remove(tenantKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void atomicUpdateTaskSetForLocation(String tenantId, String location, Function<TaskSet, TaskSet> updateOp) {
        TenantKey tenantKey = new TenantKey(tenantId, location);

        Lock lock = taskSetIgniteCache.lock(tenantKey);
        lock.lock();
        try {
            var currentTaskSet = taskSetIgniteCache.get(tenantKey);

            LOG.debug("Calling update function in (distributed) critical section - STARTED");
            var updatedTaskSet = updateOp.apply(currentTaskSet);
            LOG.debug("Calling update function in (distributed) critical section - FINISHED");

            // NOTE the rare instance equality check.  This is intentional.
            if (updatedTaskSet != currentTaskSet) {
                if (updatedTaskSet != null) {
                    LOG.debug("Updating task set after operation complete: tenant-id={}; location={}", tenantId, location);
                    taskSetIgniteCache.put(tenantKey, updatedTaskSet);
                } else {
                    LOG.debug("Removing task set on update operation return null: tenant-id={}; location={}",
                        tenantId, location);
                    taskSetIgniteCache.remove(tenantKey);
                }
            } else {
                LOG.debug("Skipping task set update - returned task set is the original: tenant-id={}; location={}",
                    tenantId, location);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addAllTwinPublisherSessionListener(TaskSetStorageListener listener) {
        LOG.debug("Registering listener for all TaskSet updates: listener={}", System.identityHashCode(listener));

        var listenerFactory = new TaskSetTwinCacheListenerFactory(listener);

        MutableCacheEntryListenerConfiguration<TenantKey, TaskSet> listenerConfiguration =
            new MutableCacheEntryListenerConfiguration<>(
                listenerFactory,
                null,
                false,
                false
            );

        MutableCacheEntryListenerConfiguration<TenantKey, TaskSet> oldListener;

        synchronized (lock) {
            oldListener = cacheListenerConfigForPublisherSession.putIfAbsent(listener, listenerConfiguration);
        }

        // Register only if it was not already registered
        if (oldListener == null) {
            taskSetIgniteCache.registerCacheEntryListener(listenerConfiguration);
        } else {
            // Why is the same TwinPublisher.Session being registered twice?
            LOG.warn("Internal error - publisher session is already registered to receive cache events");
        }
    }

    @Override
    public void removeTwinPublisherListener(TaskSetStorageListener listener) {
        LOG.debug("Removing listener for TaskSet: listener={}", System.identityHashCode(listener));

        MutableCacheEntryListenerConfiguration<TenantKey, TaskSet> oldListener;

        synchronized (lock) {
            oldListener = cacheListenerConfigForPublisherSession.get(listener);
        }

        if (oldListener != null) {
            taskSetIgniteCache.deregisterCacheEntryListener(oldListener);
        } else {
            // Why is this TwinPublisher.session being removed when it was not registered?
            LOG.warn("Internal error - publisher session is not registered on attempt to remove");
        }
    }
}
