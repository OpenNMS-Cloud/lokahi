package org.opennms.horizon.inventory.grpc.taskset;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
public class TestTaskSetGrpcService extends TaskSetServiceGrpc.TaskSetServiceImplBase {
    private final AtomicInteger timesCalled = new AtomicInteger(0);
    private final List<PublishTaskSetRequest> requests = new ArrayList<>();

    @Override
    public void publishTaskSet(PublishTaskSetRequest request,
                               StreamObserver<PublishTaskSetResponse> responseObserver) {
        this.timesCalled.incrementAndGet();
        this.requests.add(request);
        log.info("Called TestTaskSetGrpcService.publishTaskSet with request = {}", request);
        responseObserver.onNext(PublishTaskSetResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public void reset() {
        timesCalled.set(0);
        requests.clear();
    }
}
