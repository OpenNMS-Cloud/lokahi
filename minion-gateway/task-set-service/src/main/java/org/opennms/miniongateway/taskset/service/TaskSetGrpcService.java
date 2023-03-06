package org.opennms.miniongateway.taskset.service;

import io.grpc.stub.StreamObserver;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API Endpoints exposed to other services to perform operations on Task Sets, such as adding and removing Tasks from
 * a TaskSet.
 *
 *  This is the INGRESS part of task set management flow:
 *      1. (INGRESS) updates received from other services, such as inventory
 *      2. (STORE + AGGREGATE) task set updates made against the Task Set store
 *      3. (EGRESS) updates pushed downstream to Minions via Twin
 */
@Component
public class TaskSetGrpcService extends TaskSetServiceGrpc.TaskSetServiceImplBase {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetGrpcService.class);

    private Logger LOG = DEFAULT_LOGGER;

    @Autowired
    @Qualifier("internalGrpcIpcServer")
    private GrpcIpcServer grpcIpcServer;

    @Autowired
    private TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;

    @Autowired
    private TaskSetStorage taskSetStorage;

//========================================
// Lifecycle
//----------------------------------------

    @PostConstruct
    public void start() throws IOException {
        grpcIpcServer.startServer(this);
        LOG.info("Initiated TaskSet GRPC Service");
    }

//========================================
// Service API
//----------------------------------------

    /**
     * Update the requested task set with the given list of task updates.  Note that removals are processed first followed
     * by additions.
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void updateTasks(UpdateTasksRequest request, StreamObserver<UpdateTasksResponse> responseObserver) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();

        AtomicInteger numAdded = new AtomicInteger(0);
        AtomicInteger numRemoved = new AtomicInteger(0);

        taskSetStorage.atomicUpdateTaskSetForLocation(
            tenantId, request.getLocation(), (original) -> {
                //
                // CRITICAL SECTION
                //
                //  WARNING - this critical section is called with a distributed lock.  Keep it short and sweet.
                //
                return this.applyUpdateToStoredTaskSet(tenantId, request, original, numAdded, numRemoved);
            });

        UpdateTasksResponse response =
            UpdateTasksResponse.newBuilder()
                .setNumAdded(numAdded.get())
                .setNumRemoved(numRemoved.get())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

//========================================
// Internal Methods
//----------------------------------------

    /**
     * Process the updates given to the Task Set.  Note that existing task definitions with the same IDs as added ones
     *  are replaced by the new ones.
     *
     * @param tenantId ID of the Tenant to which the Task Set belongs.
     * @param request details of the update request.
     * @param original TaskSet to update.
     * @param numAdded stores the total number of tasks added on completion
     * @param numRemoved stores the total number of tasks removed on completion; not this includes any existing tasks
     *                  replaced by new ones that were added.
     * @return updated Task Set, or the original Task Set if no changes were actually made
     */
    private TaskSet
    applyUpdateToStoredTaskSet(
        String tenantId,
        UpdateTasksRequest request,
        TaskSet original,
        AtomicInteger numAdded,
        AtomicInteger numRemoved) {

        int removed = 0;
        int added = 0;
        TaskSet.Builder updatedTaskSetBuilder = TaskSet.newBuilder();

        // First scan the requested updates and collect all of the removal task IDs.  NOTE: also includes all of the IDs
        //  for tasks that are being added to prevent duplicate task IDs in the end result.
        var removeTaskIDs = collectRemovalIds(request);

        if (original != null) {
            // Next scan the task set and remove those requested tasks
            removed = copyExistingTasksWithFilter(removeTaskIDs, original, updatedTaskSetBuilder);
        }

        // Finally, add any new tasks
        added = addNewTasks(request, updatedTaskSetBuilder);

        LOG.debug("Remove tasks: tenant={}; location={}; added-count={}; removed-count={}",
            tenantId, request.getLocation(), added, removed);

        // Determine the return value based on whether there was an actual change
        TaskSet result;
        if ((removed > 0) || (added > 0)) {
            // Return the new, updated task set
            numAdded.set(added);
            numRemoved.set(removed);
            result = updatedTaskSetBuilder.build();
        } else {
            // Return the original task set so the storage can ignore the update
            result = original;
        }

        return result;
    }

    private Set<String> collectRemovalIds(UpdateTasksRequest request) {
        Set<String> result = new HashSet<>();

        for (var update : request.getUpdateList()) {
            if (update.hasRemoveTask()) {
                result.add(update.getRemoveTask().getTaskId());
            } else if (update.hasAddTask()) {
                // Include the "Add Task" ids so existing tasks with the same IDs are removed first.
                result.add(update.getAddTask().getTaskDefinition().getId());
            }
        }

        return result;
    }

    private int copyExistingTasksWithFilter(Set<String> removeTaskIDs, TaskSet original, TaskSet.Builder updatedTaskSetBuilder) {
        int removed = 0;
        for (var taskDefinition : original.getTaskDefinitionList()) {
            if (!removeTaskIDs.contains(taskDefinition.getId())) {
                updatedTaskSetBuilder.addTaskDefinition(taskDefinition);
            } else {
                removed++;
            }
        }

        return removed;
    }

    private int addNewTasks(UpdateTasksRequest request, TaskSet.Builder updatedTaskSetBuilder) {
        int added = 0;

        for (var update : request.getUpdateList()) {
            if (update.hasAddTask()) {
                updatedTaskSetBuilder.addTaskDefinition((update.getAddTask().getTaskDefinition()));
                added++;
            }
        }

        return added;
    }
}
