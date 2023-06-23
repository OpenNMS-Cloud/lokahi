package org.opennms.miniongateway.taskset.service;

import lombok.Getter;
import org.opennms.taskset.contract.TaskSet;

import java.util.HashSet;
import java.util.Set;

import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process task set updates in the TaskSetGrpcService.
 */
public class TaskSetGrpcServiceUpdateProcessor implements TaskSetStorageUpdateFunction {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetGrpcServiceUpdateProcessor.class);

    /**
     * The request with updates to apply to the task set.
     */
    private final UpdateTasksRequest updateTasksRequest;

    /**
     * Number of task definitions added to the task set by this processor, updated on completion of the processor.
     */
    @Getter
    private int numNew;

    /**
     * Number of task definitions replaced in the task set by this processor, updated on completion of the processor.
     * Replaced tasks are tasks that existed in the task set but were removed while a replacement with the same ID was
     * also added.
     */
    @Getter
    private int numReplaced;

    /**
     * Number of existing task definitions removed from the task set by this processor, updated on completion of the processor.
     */
    @Getter
    private int numRemoved;

    private Set<String> requestedAddIds;
    private Set<String> requestedRemovalIds;
    private Set<String> replacedIds;

//========================================
// Constructor
//----------------------------------------

    public TaskSetGrpcServiceUpdateProcessor(UpdateTasksRequest updateTasksRequest) {
        this.updateTasksRequest = updateTasksRequest;
    }

//========================================
// Processor
//----------------------------------------

    /**
     * CRITICAL SECTION WARNING - this method is called with a distributed lock held.  Keep it short and sweet.
     *
     * Process the updates given to the Task Set.  Note that existing task definitions with the same IDs as added ones
     *  are replaced by the new ones.
     *
     * @param original TaskSet to update.
     * @return updated Task Set, or the original Task Set if no changes were actually made; this operation never removes
     * the task set, even if the end result has 0 tasks, so null is never returned.
     */
    @Override
    public TaskSet process(TaskSet original) {
        requestedAddIds = new HashSet<>();
        requestedRemovalIds = new HashSet<>();
        replacedIds = new HashSet<>();

        TaskSet.Builder updatedTaskSetBuilder = TaskSet.newBuilder();

        // Extract the IDs for both additions and removals first.
        extractRequestedAddRemovalIds(updateTasksRequest);

        if (original != null) {
            // Scan the original task set and copy out existing tasks that will be retained
            copyExistingTasksWithFilter(original, updatedTaskSetBuilder);
        }

        // Finally, add any new tasks
        addNewTasks(updateTasksRequest, updatedTaskSetBuilder);

        LOG.debug("Remove tasks: tenantId={}; locationId={}; added-count={}; replaced-count={}; removed-count={}",
            updateTasksRequest.getTenantId(), updateTasksRequest.getLocationId(), numNew, numReplaced, numRemoved);

        // Determine the return value based on whether there was an actual change
        TaskSet result;
        if ((numRemoved > 0) || (numNew > 0) || (numReplaced > 0)) {
            // Return the new, updated task set
            result = updatedTaskSetBuilder.build();
        } else {
            // Return the original task set so the storage can ignore the update
            result = original;
        }

        return result;
    }

//========================================
// Internals
//----------------------------------------

    private void extractRequestedAddRemovalIds(UpdateTasksRequest request) {

        request.getUpdateList().forEach(
            (update) -> {
                if (update.hasAddTask()) {
                    requestedAddIds.add(update.getAddTask().getTaskDefinition().getId());
                } else if (update.hasRemoveTask()) {
                    requestedRemovalIds.add(update.getRemoveTask().getTaskId());
                } else {
                    LOG.error("Ignoring unrecognized update request with no add-task and no remove-task: tenantId={}; locationId={}",
                        request.getTenantId(), request.getLocationId());
                }
            }
        );
    }

    private void copyExistingTasksWithFilter(TaskSet original, TaskSet.Builder updatedTaskSetBuilder) {
        for (var taskDefinition : original.getTaskDefinitionList()) {
            String taskId = taskDefinition.getId();

            if (requestedRemovalIds.contains(taskId)) {
                // Discard - it's being explicitly removed
                numRemoved++;
            } else if (requestedAddIds.contains(taskId)) {
                // Discard - it will be replaced by an new task definition with the same ID
                numReplaced++;
                replacedIds.add(taskId);
            } else {
                updatedTaskSetBuilder.addTaskDefinition(taskDefinition);
            }
        }
    }

    private void addNewTasks(UpdateTasksRequest request, TaskSet.Builder updatedTaskSetBuilder) {
        for (var update : request.getUpdateList()) {
            if (update.hasAddTask()) {
                var taskDefinition = update.getAddTask().getTaskDefinition();
                updatedTaskSetBuilder.addTaskDefinition(taskDefinition);

                // Update the add count, if this wasn't already counted as a replacement.
                if (! replacedIds.contains(taskDefinition.getId())) {
                    numNew++;
                }
            }
        }
    }
}
