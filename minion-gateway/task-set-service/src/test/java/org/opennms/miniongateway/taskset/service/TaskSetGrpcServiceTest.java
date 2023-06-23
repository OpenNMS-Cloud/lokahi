package org.opennms.miniongateway.taskset.service;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.RemoveSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

class TaskSetGrpcServiceTest {
    private TaskSetGrpcService target;

    private GrpcIpcServer mockGrpcIpcServer;
    private TaskSetStorage mockTaskSetStorage;
    private StreamObserver<UpdateTasksResponse> mockUpdateTasksRepsonseStreamObserver;
    private TaskSetGrpcServiceUpdateProcessor mockTaskSetGrpcServiceUpdateProcessor;
    private TaskSetGrpcServiceUpdateProcessorFactory mockTaskSetGrpcServiceUpdateProcessorFactory;

    private TaskDefinition testNewTaskDefinition;
    private AddSingleTaskOp testAddSingleTaskOp;
    private RemoveSingleTaskOp testRemoveSingleTaskOp;
    private UpdateTasksRequest testRequest;


    @BeforeEach
    void setUp() {
        testNewTaskDefinition =
            TaskDefinition.newBuilder()
                .setId("x-task-001-id-x")
                .setPluginName("x-plugin-name-x")
                .build();

        testAddSingleTaskOp =
            AddSingleTaskOp.newBuilder()
                .setTaskDefinition(testNewTaskDefinition)
                .build();

        testRemoveSingleTaskOp =
            RemoveSingleTaskOp.newBuilder()
                .setTaskId("x-task-002-id-x")
                .build();

        testRequest =
            UpdateTasksRequest.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("505050")
                .addUpdate(UpdateSingleTaskOp.newBuilder().setAddTask(testAddSingleTaskOp).build())
                .addUpdate(UpdateSingleTaskOp.newBuilder().setRemoveTask(testRemoveSingleTaskOp).build())
                .build();

        mockGrpcIpcServer = Mockito.mock(GrpcIpcServer.class);
        mockTaskSetStorage = Mockito.mock(TaskSetStorage.class);
        mockUpdateTasksRepsonseStreamObserver = Mockito.mock(StreamObserver.class);
        mockTaskSetGrpcServiceUpdateProcessor = Mockito.mock(TaskSetGrpcServiceUpdateProcessor.class);
        mockTaskSetGrpcServiceUpdateProcessorFactory = Mockito.mock(TaskSetGrpcServiceUpdateProcessorFactory.class);

        target = new TaskSetGrpcService();

        Mockito.when(mockTaskSetGrpcServiceUpdateProcessorFactory.create(testRequest)).thenReturn(mockTaskSetGrpcServiceUpdateProcessor);
    }

    @Test
    void testStart() throws IOException {
        //
        // Execute
        //
        target.setGrpcIpcServer(mockGrpcIpcServer);
        target.start();

        //
        // Verify the Results
        //
        Mockito.verify(mockGrpcIpcServer).startServer(target);
    }

    @Test
    void testUpdateTasksSuccess() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockTaskSetGrpcServiceUpdateProcessor.getNumNew()).thenReturn(13);
        Mockito.when(mockTaskSetGrpcServiceUpdateProcessor.getNumRemoved()).thenReturn(15);
        Mockito.when(mockTaskSetGrpcServiceUpdateProcessor.getNumReplaced()).thenReturn(17);

        //
        // Execute
        //
        target.setGrpcIpcServer(mockGrpcIpcServer);
        target.setTaskSetStorage(mockTaskSetStorage);
        target.setTaskSetGrpcServiceUpdateProcessorFactory(mockTaskSetGrpcServiceUpdateProcessorFactory);
        target.updateTasks(testRequest, mockUpdateTasksRepsonseStreamObserver);

        //
        // Verify the Results
        //
        InOrder inOrder = Mockito.inOrder(mockUpdateTasksRepsonseStreamObserver);

        // The response counts are both zero because the callback passed to atomicUpdateTaskSetForLocation has not
        //  been executed.
        inOrder.verify(mockUpdateTasksRepsonseStreamObserver).onNext(Mockito.argThat((response) -> response.getNumNew() == 13 && response.getNumRemoved() == 15 && response.getNumReplaced() == 17));
        inOrder.verify(mockUpdateTasksRepsonseStreamObserver).onCompleted();
    }

    @Test
    void testUpdateTasksException() {
        //
        // Setup Test Data and Interactions
        //
        RuntimeException testException = new RuntimeException("x-test-exception-x");
        Mockito.doThrow(testException).when(mockTaskSetStorage).atomicUpdateTaskSetForLocation("x-tenant-id-x", "505050", mockTaskSetGrpcServiceUpdateProcessor);

        //
        // Execute
        //
        target.setGrpcIpcServer(mockGrpcIpcServer);
        target.setTaskSetStorage(mockTaskSetStorage);
        target.setTaskSetGrpcServiceUpdateProcessorFactory(mockTaskSetGrpcServiceUpdateProcessorFactory);

        RuntimeException actualException = null;
        try {
            target.updateTasks(testRequest, mockUpdateTasksRepsonseStreamObserver);
            fail("missing expected exception");
        } catch (RuntimeException thrown) {
            actualException = thrown;
        }

        //
        // Verify the Results
        //
        assertSame(testException, actualException);
        Mockito.verifyNoInteractions(mockUpdateTasksRepsonseStreamObserver);
    }
}
