package org.opennms.horizon.inventory.grpc.taskset;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;

@Slf4j
@Getter
public class TestTaskSetGrpcService extends TaskSetServiceGrpc.TaskSetServiceImplBase {
    private int timesCalled = 0;

    @Override
    public void publishTaskSet(PublishTaskSetRequest request,
                               StreamObserver<PublishTaskSetResponse> responseObserver) {
        timesCalled++;
        log.info("Called TestTaskSetGrpcService.publishTaskSet with request = {}", request);
        responseObserver.onNext(PublishTaskSetResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void reset() {
        timesCalled = 0;
    }
}
