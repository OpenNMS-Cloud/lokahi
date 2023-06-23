package org.opennms.miniongateway.ignite;

import com.google.protobuf.Any;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.RemoveSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;

/**
 * DEVELOPER TEST - only executed manually
 * 
 * Tests to produce GRPC JSON for testing (e.g. for use with grpcurl)
 */
public class GrpcJsonDT {

    @Test
    public void testGrpcJsonUpdateTaskSetOp() throws Exception {

        UpdateSingleTaskOp addOpWrapper = prepareAddOp();

        UpdateSingleTaskOp removeOpWrapper = prepareRemoveOp();

        UpdateTasksRequest updateTasksRequest =
            UpdateTasksRequest.newBuilder()
                .setLocationId("x-location-x")
                .addUpdate(addOpWrapper)
                .addUpdate(removeOpWrapper)
                .build();

        TypeRegistry typeRegistry =
            TypeRegistry.newBuilder()
                .add(UpdateTasksRequest.getDescriptor())
                .add(IcmpMonitorRequest.getDescriptor())
                .build();

        String json = JsonFormat.printer().usingTypeRegistry(typeRegistry).print(updateTasksRequest);

        System.out.println(json);
    }

    @Test
    public void testGrpcJsonPublishTaskSetOp() throws Exception {

        TaskDefinition taskDefinition = prepareTaskDefinition();

        TaskSet taskSet =
            TaskSet.newBuilder()
                .addTaskDefinition(taskDefinition)
                .build();

        PublishTaskSetRequest publishTaskSetRequest =
            PublishTaskSetRequest.newBuilder()
                .setLocationId("x-location-x")
                .setTaskSet(taskSet)
                .build();

        TypeRegistry typeRegistry =
            TypeRegistry.newBuilder()
                .add(UpdateTasksRequest.getDescriptor())
                .add(IcmpMonitorRequest.getDescriptor())
                .build();

        String json = JsonFormat.printer().usingTypeRegistry(typeRegistry).print(publishTaskSetRequest);

        System.out.println(json);
    }

    @NotNull
    private UpdateSingleTaskOp prepareRemoveOp() {
        RemoveSingleTaskOp removeOp =
            RemoveSingleTaskOp.newBuilder()
                .setTaskId("x-remove-task-id-x")
                .build();

        UpdateSingleTaskOp removeOpWrapper =
            UpdateSingleTaskOp.newBuilder()
                .setRemoveTask(removeOp)
                .build();
        return removeOpWrapper;
    }

    @NotNull
    private UpdateSingleTaskOp prepareAddOp() {
        TaskDefinition taskDefinition = prepareTaskDefinition();

        AddSingleTaskOp addOp =
            AddSingleTaskOp.newBuilder()
                .setTaskDefinition(taskDefinition)
                .build();

        UpdateSingleTaskOp addOpWrapper =
            UpdateSingleTaskOp.newBuilder()
                .setAddTask(addOp)
                .build();

        return addOpWrapper;
    }

    @NotNull
    private TaskDefinition prepareTaskDefinition() {
        IcmpMonitorRequest icmpMonitorRequest =
            IcmpMonitorRequest.newBuilder()
                .setHost("127.0.0.1")
                .build();

        Any taskConfig = Any.pack(icmpMonitorRequest);

        TaskDefinition taskDefinition =
            TaskDefinition.newBuilder()
                .setId("x-task-id-001-x")
                .setPluginName("ICMP")
                .setConfiguration(taskConfig)
                .build();
        return taskDefinition;
    }

}
