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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.opennms.miniongateway.taskset.service.TaskSetStorageListener;
import org.opennms.taskset.contract.TaskSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for updates to TaskSets in the Ignite Cache that forwards updates to Twin subscriptions.
 *
 * WARNING: the onUpdated method is called
 */
public class TaskSetTwinCacheListener
        implements CacheEntryUpdatedListener<TenantKey, TaskSet>, CacheEntryCreatedListener<TenantKey, TaskSet> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetTwinCacheListener.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final TaskSetStorageListener downstreamSession;

    public TaskSetTwinCacheListener(TaskSetStorageListener downstreamSession) {
        this.downstreamSession = downstreamSession;
    }

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends TenantKey, ? extends TaskSet>> cacheEntryEvents)
            throws CacheEntryListenerException {
        LOG.debug("Have new task-set entry");
        commonTaskSetCacheEntryEventHandling(cacheEntryEvents);
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends TenantKey, ? extends TaskSet>> cacheEntryEvents)
            throws CacheEntryListenerException {
        LOG.debug("Have update for task-sets");
        commonTaskSetCacheEntryEventHandling(cacheEntryEvents);
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void commonTaskSetCacheEntryEventHandling(
            Iterable<CacheEntryEvent<? extends TenantKey, ? extends TaskSet>> cacheEntryEvents) {
        Map<TenantKey, TaskSet> dedupMap = new HashMap<>();

        for (var oneEvent : cacheEntryEvents) {
            var tenantLocation = oneEvent.getKey();
            TaskSet taskSet = oneEvent.getValue();

            // Put the last one over any prior ones; only need to send a single notification downstream per Tenant
            // Location.
            dedupMap.put(tenantLocation, taskSet);
        }

        // Process all of the Tenant Location updates
        for (var entry : dedupMap.entrySet()) {
            var tenantLocation = entry.getKey();
            publishUpdateForTenantLocation(tenantLocation.getTenantId(), tenantLocation.getKey(), entry.getValue());
        }
    }

    private void publishUpdateForTenantLocation(String tenantId, String location, TaskSet taskSet)
            throws CacheEntryListenerException {
        LOG.debug(
                "Have update for task-set: tenant-id={}; location={}; num-task={}",
                tenantId,
                location,
                taskSet.getTaskDefinitionCount());

        try {
            downstreamSession.publish(tenantId, location, taskSet);
        } catch (IOException ioException) {
            LOG.error("Failed to update twin subscriber on taskset update", ioException);
        }
    }
}
