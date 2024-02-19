/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.miniongateway.taskset.internal;

import java.util.IdentityHashMap;
import java.util.concurrent.locks.Lock;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.opennms.miniongateway.taskset.service.TaskSetStorage;
import org.opennms.miniongateway.taskset.service.TaskSetStorageListener;
import org.opennms.miniongateway.taskset.service.TaskSetStorageUpdateFunction;
import org.opennms.taskset.contract.TaskSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    public TaskSet getTaskSetForLocation(String tenantId, String locationId) {
        TenantKey tenantKey = new TenantKey(tenantId, locationId);

        return taskSetIgniteCache.get(tenantKey);
    }

    @Override
    public void putTaskSetForLocation(String tenantId, String locationId, TaskSet taskSet) {
        TenantKey tenantKey = new TenantKey(tenantId, locationId);

        taskSetIgniteCache.put(tenantKey, taskSet);
    }

    @Override
    public boolean deleteTaskSetForLocation(String tenantId, String locationId) {
        TenantKey tenantKey = new TenantKey(tenantId, locationId);

        return taskSetIgniteCache.remove(tenantKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void atomicUpdateTaskSetForLocation(
            String tenantId, String locationId, TaskSetStorageUpdateFunction updateFunction) {
        TenantKey tenantKey = new TenantKey(tenantId, locationId);

        Lock lock = taskSetIgniteCache.lock(tenantKey);
        lock.lock();
        try {
            var currentTaskSet = taskSetIgniteCache.get(tenantKey);

            LOG.debug("Calling update function in (distributed) critical section - STARTED");
            var updatedTaskSet = updateFunction.process(currentTaskSet);
            LOG.debug("Calling update function in (distributed) critical section - FINISHED");

            // NOTE the rare instance equality check.  This is intentional.
            if (updatedTaskSet != currentTaskSet) {
                if (updatedTaskSet != null) {
                    LOG.debug(
                            "Updating task set after operation complete: tenantId={}; locationId={}",
                            tenantId,
                            locationId);
                    taskSetIgniteCache.put(tenantKey, updatedTaskSet);
                } else {
                    LOG.debug(
                            "Removing task set on update operation return null: tenantId={}; locationId={}",
                            tenantId,
                            locationId);
                    taskSetIgniteCache.remove(tenantKey);
                }
            } else {
                LOG.debug(
                        "Skipping task set update - returned task set is the original: tenantId={}; locationId={}",
                        tenantId,
                        locationId);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTaskSetStorageListener(TaskSetStorageListener listener) {
        LOG.debug("Registering listener for all TaskSet updates: listener={}", System.identityHashCode(listener));

        var listenerFactory = new TaskSetTwinCacheListenerFactory(listener);

        MutableCacheEntryListenerConfiguration<TenantKey, TaskSet> listenerConfiguration =
                new MutableCacheEntryListenerConfiguration<>(listenerFactory, null, false, false);

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
}
