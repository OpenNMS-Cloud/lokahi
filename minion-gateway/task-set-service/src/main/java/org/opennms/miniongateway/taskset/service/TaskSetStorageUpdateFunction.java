package org.opennms.miniongateway.taskset.service;

import org.opennms.taskset.contract.TaskSet;

public interface TaskSetStorageUpdateFunction {
    /**
     * Process the original task set given and return the updated task set to store.
     *
     * CRITICAL SECTION WARNING: this method is called with a distributed lock held.  Keep implementations short and sweet.
     *
     * @param original copy of the task set from storage.
     * @return (1) the updated task set, (2) the original to indicate that no changes need to be stored, or (3) null
     *         to indicate the task set should be removed.
     */
    TaskSet process(TaskSet original);
}
