package org.opennms.miniongateway.taskset.service;

import org.opennms.taskset.contract.TaskSet;

import java.util.function.Function;

public interface TaskSetStorage {
    TaskSet getTaskSetForLocation(String tenantId, String locationId);
    void putTaskSetForLocation(String tenantId, String locationId, TaskSet taskSet);
    boolean deleteTaskSetForLocation(String tenantId, String locationId);

    /**
     * Atomically update the task set for the given Tenant Location.  The given update operation function is called
     *  with a distributed lock on the entry.
     *
     *      - It is VERY IMPORTANT that the updateOp execute quickly as it is called in a critical section, not to
     *        mention that the lock is distributed, so contention across multiple service instances is a concern.
     *
     * @param tenantId ID of the Tenant to which the Task Set belongs.
     * @param locationId location to which the Task Set belongs.
     * @param updateFunction function that receives the original Task Set for the location (or null if none currently
     *                       exists) and returns one of the following: (1) null to indicate the task set should be
     *                       removed; (2) the original task set itself, in which case the cache is not updated; (3) a
     *                       new TaskSet instance, in which case the cache entry is updated to the new instance.
     */
    void atomicUpdateTaskSetForLocation(String tenantId, String locationId, TaskSetStorageUpdateFunction updateFunction);

    /**
     * Add a listener for all TaskSet updates.
     *
     * @param listener
     */
    void addTaskSetStorageListener(TaskSetStorageListener listener);
}
