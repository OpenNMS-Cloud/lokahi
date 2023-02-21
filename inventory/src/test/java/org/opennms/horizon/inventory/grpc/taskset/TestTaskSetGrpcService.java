package org.opennms.horizon.inventory.grpc.taskset;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Getter
public class TestTaskSetGrpcService extends TaskSetServiceGrpc.TaskSetServiceImplBase {

    private final List<PublishTaskSetRequest> requests = new ArrayList<>();
    private final Map<String, Set<TaskDefinition>> taskSetMap = new HashMap<>();

    @Override
    public synchronized void publishTaskSet(PublishTaskSetRequest request,
                               StreamObserver<PublishTaskSetResponse> responseObserver) {
        this.requests.add(request);
        taskSetMap.put(request.getLocation(), new HashSet<>(request.getTaskSet().getTaskDefinitionList()));
        log.info("Called TestTaskSetGrpcService.publishTaskSet with request = {}", request);
        responseObserver.onNext(PublishTaskSetResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void reset() {
        requests.clear();
        taskSetMap.clear();
    }

    public Set<TaskDefinition> getTaskDefinitions(String location) {
        var taskSet = taskSetMap.get(location);
        if (taskSet == null) {
            return new HashSet<>();
        }
        return taskSet;
    }

}
