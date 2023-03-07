package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.ipc;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.testtool.miniongateway.wiremock.api.ServiceApiForMinionGatewayWiremock;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MockTaskSetService extends TaskSetServiceGrpc.TaskSetServiceImplBase implements ServiceApiForMinionGatewayWiremock {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MockTaskSetService.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final List<UpdateTasksRequest> receivedTaskSetUpdates = new ArrayList<>();

    private final Object lock = new Object();

//========================================
// Getters and Setters
//----------------------------------------

    public List<UpdateTasksRequest> getReceivedTaskSetUpdates() {
        return receivedTaskSetUpdates;
    }


//========================================
// GRPC Service Endpoints
//----------------------------------------

    @Override
    public void updateTasks(UpdateTasksRequest request, StreamObserver<UpdateTasksResponse> responseObserver) {
        LOG.info("MOCK TASK SET SERVICE: have update-tasks-request: request={}", request);

        synchronized (lock) {
            this.receivedTaskSetUpdates.add(request);
        }

        responseObserver.onNext(
            UpdateTasksResponse.newBuilder()
                .setNumAdded(11)    // TODO: do we need more realistic numbers?
                .setNumRemoved(13)  // TODO: do we need more realistic numbers?
                .build()
        );
        responseObserver.onCompleted();
    }
}
