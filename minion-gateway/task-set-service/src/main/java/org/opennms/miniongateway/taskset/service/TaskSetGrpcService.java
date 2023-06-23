package org.opennms.miniongateway.taskset.service;

import io.grpc.stub.StreamObserver;
import lombok.Setter;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
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
    @Setter // Testability
    private GrpcIpcServer grpcIpcServer;

    @Autowired
    @Setter // Testability
    private TaskSetStorage taskSetStorage;

    @Autowired
    @Setter // Testability
    private TaskSetGrpcServiceUpdateProcessorFactory taskSetGrpcServiceUpdateProcessorFactory;

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
        TaskSetGrpcServiceUpdateProcessor updateProcessor = taskSetGrpcServiceUpdateProcessorFactory.create(request);

        try {
            taskSetStorage.atomicUpdateTaskSetForLocation(request.getTenantId(), request.getLocationId(), updateProcessor);
        } catch (RuntimeException rtExc) {
            // Log exceptions here that might otherwise get swallowed
            LOG.warn("error applying task set updates", rtExc);
            throw rtExc;
        }

        UpdateTasksResponse response =
            UpdateTasksResponse.newBuilder()
                .setNumNew(updateProcessor.getNumNew())
                .setNumReplaced(updateProcessor.getNumReplaced())
                .setNumRemoved(updateProcessor.getNumRemoved())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
