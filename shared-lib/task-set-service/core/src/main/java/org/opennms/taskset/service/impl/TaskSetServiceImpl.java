package org.opennms.taskset.service.impl;

import org.opennms.taskset.model.TaskSet;
import org.opennms.taskset.service.api.TaskSetListener;
import org.opennms.taskset.service.api.TaskSetService;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Process task set updates, publishing them to downstream minions and storing the latest version to provide to minions
 *  on request.
 */
public class TaskSetServiceImpl implements TaskSetService {

    private Map<String, TaskSet> taskSetByLocation = new HashMap<>();
    private Map<String, Set<TaskSetListener>> taskSetListeners = new HashMap<>();

    private final Object lock = new Object();

//========================================
// Interface
//----------------------------------------

    public void publishTaskSet(String location, TaskSet taskSet) {
        Set<TaskSetListener> listeners;

        synchronized (lock) {
            taskSetByLocation.put(location, taskSet);
            listeners = taskSetListeners.get(location);
        }

        // Publish to downstream listeners
        if (listeners != null) {
            for (TaskSetListener listener : listeners) {
                listener.onTaskSetUpdate(taskSet);
            }
        }
    }

    @Override
    public void addListener(String location, TaskSetListener listener) {
        boolean added;
        TaskSet latestDefinition = null;

        synchronized (lock) {
            Set<TaskSetListener> listeners = taskSetListeners.computeIfAbsent(location, (key) -> this.createIdentitySet());
            added = listeners.add(listener);
            if (added) {
                latestDefinition = taskSetByLocation.get(location);
            }
        }

        // If the listener is newly added and a task-set definition exists for the location, send it now.
        if ((added) && (latestDefinition != null)) {
            listener.onTaskSetUpdate(latestDefinition);
        }
    }

    @Override
    public void removeListener(String location, TaskSetListener listener) {
        synchronized (lock) {
            Set<TaskSetListener> listeners = taskSetListeners.get(location);
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
