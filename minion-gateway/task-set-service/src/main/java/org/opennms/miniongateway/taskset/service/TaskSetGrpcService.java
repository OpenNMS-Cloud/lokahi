package org.opennms.miniongateway.taskset.service;

import io.grpc.stub.StreamObserver;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class TaskSetGrpcService extends TaskSetServiceGrpc.TaskSetServiceImplBase {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetGrpcService.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    private TaskSetPublisher taskSetPublisher;

    @Autowired
    @Qualifier("internalGrpcIpcServer")
    private GrpcIpcServer grpcIpcServer;

    @Autowired
    private TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;

//========================================
// Lifecycle
//----------------------------------------

    @PostConstruct
    public void start() throws IOException {
        grpcIpcServer.startServer(this);
        log.info("Started TaskSet GRPC Service");
    }

//========================================
// Service API
//----------------------------------------

    @Override
    public void publishTaskSet(PublishTaskSetRequest request, StreamObserver<PublishTaskSetResponse> responseObserver) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();

        taskSetPublisher.publishTaskSet(tenantId, request.getLocation(), request.getTaskSet());

        PublishTaskSetResponse response =
            PublishTaskSetResponse.newBuilder()
                .build()
            ;

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
