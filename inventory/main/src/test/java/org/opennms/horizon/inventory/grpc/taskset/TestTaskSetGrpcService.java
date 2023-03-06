package org.opennms.horizon.inventory.grpc.taskset;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Getter
public class TestTaskSetGrpcService extends TaskSetServiceGrpc.TaskSetServiceImplBase {

    private final List<UpdateTasksRequest> requests = new LinkedList<>();
    private final Map<String, Map<String, TaskDefinition>> taskSetMap = new HashMap<>();

    @Override
    public void updateTasks(UpdateTasksRequest request, StreamObserver<UpdateTasksResponse> responseObserver) {
        this.requests.add(request);

        // Apply the updates to the taskset for the location
        applyTaskSetUpdatesForLocation(request.getLocation(), request.getUpdateList());

        log.info("Called TestTaskSetGrpcService.updateTasks with request = {}", request);
        responseObserver.onNext(UpdateTasksResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void reset() {
        requests.clear();
    }

    // TBD888: change the tests to verify the individual updates instead of the end result of the updates
    public Set<TaskDefinition> getTaskDefinitions(String location) {
        var taskSet = taskSetMap.get(location);

        if (taskSet == null) {
            return new HashSet<>();
        }

        return new HashSet<>(taskSet.values());
    }

//========================================
// Internals
//----------------------------------------

    private void applyTaskSetUpdatesForLocation(String location, List<UpdateSingleTaskOp> updates) {
        Map locationMap = taskSetMap.computeIfAbsent(location, key -> new HashMap<>());

        for (UpdateSingleTaskOp singleTaskOp : updates) {
            if (singleTaskOp.hasAddTask()) {
                TaskDefinition addTaskDefinition = singleTaskOp.getAddTask().getTaskDefinition();

                locationMap.put(addTaskDefinition.getId(), addTaskDefinition);
            } else if (singleTaskOp.hasRemoveTask()) {
                locationMap.remove(singleTaskOp.getRemoveTask().getTaskId());
            }
        }
    }
}
