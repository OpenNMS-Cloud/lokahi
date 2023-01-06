/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.miniongateway.taskset;

import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.api.TaskSetForwarder;
import org.opennms.taskset.service.api.TaskSetListener;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Process task set updates, publishing them to downstream minions and storing the latest version to provide to minions
 *  on request.
 */
public class TaskSetPublisherImpl implements TaskSetPublisher, TaskSetForwarder {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetPublisherImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private Map<TenantKey, TaskSet> taskSetByLocation = new HashMap<>();
    private Map<TenantKey, Set<TaskSetListener>> taskSetListeners = new HashMap<>();

    private final Object lock = new Object();

//========================================
// Interface
//----------------------------------------

    @Override
    public void publishTaskSet(String tenantId, String location, TaskSet taskSet) {
        Set<TaskSetListener> listeners;

        synchronized (lock) {
            TenantKey tenantKey = new TenantKey(tenantId, location);
            taskSetByLocation.put(tenantKey, taskSet);
            listeners = taskSetListeners.get(tenantKey);
        }

        // TODO: reduce log level to debug
        log.info("Received task set for location: location={}; num-task={}",
            location,
            Optional.ofNullable(taskSet.getTaskDefinitionList()).map(Collection::size).orElse(0));

        // Publish to downstream listeners
        // NOTE: there will not be a listener for a location
        // if a minion has not registered in that location, it will not be in this set
        // location must be a valid location with a minion
        if (listeners != null) {
            for (TaskSetListener listener : listeners) {
                listener.onTaskSetUpdate(taskSet);
            }
        }
    }

    @Override
    public void publishNewTasks(String tenantId, String location, List<TaskDefinition> taskList) {

    }

    @Override
    public void addListener(String tenantId, String location, TaskSetListener listener) {
        boolean added;
        TaskSet latestDefinition = null;

        synchronized (lock) {
            TenantKey tenantKey = new TenantKey(tenantId, location);
            Set<TaskSetListener> listeners = taskSetListeners.computeIfAbsent(tenantKey, (key) -> this.createIdentitySet());
            added = listeners.add(listener);
            if (added) {
                latestDefinition = taskSetByLocation.get(tenantKey);
            }
        }

        // If the listener is newly added and a task-set definition exists for the location, send it now.
        if ((added) && (latestDefinition != null)) {
            listener.onTaskSetUpdate(latestDefinition);
        }
    }

    @Override
    public void removeListener(String tenantId, String location, TaskSetListener listener) {
        synchronized (lock) {
            Set<TaskSetListener> listeners = taskSetListeners.get(new TenantKey(tenantId, location));
            if (listeners != null) {
                listeners.remove(listener);
            }
        }
    }

//========================================
// Internals
//----------------------------------------

    private <X> Set<X> createIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
