/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.inventory.service.taskset.publisher;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.Setter;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.RemoveSingleTaskOp;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc.TaskSetServiceBlockingStub;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class GrpcTaskSetPublisher implements TaskSetPublisher {

    public static final String TASK_SET_PUBLISH_BEAN_NAME = "taskSetServiceBlockingStub";
    private static final Logger LOG = LoggerFactory.getLogger(GrpcTaskSetPublisher.class);
    private final ManagedChannel channel;
    private final long deadline;

    // Avoid TaskSetServiceBlockingStub sadly because it is final, and hence cannot be mocked.  This is effective
    private io.grpc.stub.AbstractBlockingStub<TaskSetServiceBlockingStub> taskSetServiceStub;

    // Test helpers to overcome static method calls which are challenging, at best, to mock
    @Setter
    private Function<ManagedChannel, io.grpc.stub.AbstractBlockingStub<TaskSetServiceBlockingStub>> taskSetServiceBlockingStubSupplier = this::supplyDefaultTaskSetServiceBlockingStub;
    @Setter
    private Function<Metadata, ClientInterceptor> attachHeadersInterceptorFunction = MetadataUtils::newAttachHeadersInterceptor;

    public GrpcTaskSetPublisher(ManagedChannel channel, long deadline) {
        this.channel = channel;
        this.deadline = deadline;
    }

    public void init() {
        taskSetServiceStub = taskSetServiceBlockingStubSupplier.apply(channel);
    }

    @Override
    public void publishNewTasks(String tenantId, String location, List<TaskDefinition> taskList) {
        publishTaskSetUpdate(
            (updateBuilder) -> taskList.forEach((taskDefinition) -> addAdditionOpToTaskUpdate(updateBuilder, taskDefinition)),
            tenantId,
            location
        );
    }

    @Override
    public void publishTaskDeletion(String tenantId, String location, List<TaskDefinition> taskList) {
        publishTaskSetUpdate(
            (updateBuilder) -> taskList.forEach((taskDefinition) -> addRemovalOpToUpdate(updateBuilder, taskDefinition.getId())),
            tenantId,
            location
        );
    }

//========================================
// Internals
//----------------------------------------

    private TaskSetServiceBlockingStub supplyDefaultTaskSetServiceBlockingStub(ManagedChannel channel) {
        return TaskSetServiceGrpc.newBlockingStub(channel);
    }

    private void addAdditionOpToTaskUpdate(UpdateTasksRequest.Builder updateBuilder, TaskDefinition task) {
        AddSingleTaskOp addOp =
            AddSingleTaskOp.newBuilder()
                .setTaskDefinition(task)
                .build();

        UpdateSingleTaskOp updateOp =
            UpdateSingleTaskOp.newBuilder()
                .setAddTask(addOp)
                .build();

        updateBuilder.addUpdate(updateOp);
    }

    private void addRemovalOpToUpdate(UpdateTasksRequest.Builder updateBuilder, String taskId) {
        RemoveSingleTaskOp removeOp =
            RemoveSingleTaskOp.newBuilder()
                .setTaskId(taskId)
                .build();

        UpdateSingleTaskOp updateOp =
            UpdateSingleTaskOp.newBuilder()
                .setRemoveTask(removeOp)
                .build();

        updateBuilder.addUpdate(updateOp);
    }

    private void publishTaskSetUpdate(Consumer<UpdateTasksRequest.Builder> populateUpdateRequestOp, String tenantId, String location) {
        try {
            UpdateTasksRequest.Builder request =
                UpdateTasksRequest.newBuilder()
                    .setLocation(location)
                    ;

            populateUpdateRequestOp.accept(request);

            Metadata metadata = new Metadata();
            metadata.put(GrpcConstants.TENANT_ID_REQUEST_KEY, tenantId);

            ClientInterceptor attachHeadersInterceptor = attachHeadersInterceptorFunction.apply(metadata);

            UpdateTasksResponse response =
                taskSetServiceStub
                    .withInterceptors(attachHeadersInterceptor)
                    .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                    .updateTasks(request.build())
                ;

            LOG.info("Update tasks for TaskSet complete: tenant-id={}; location={}; response={}", tenantId, location, response);
        } catch (Exception exc) {
            LOG.error("Error updating tasks for TaskSet", exc);
            throw new RuntimeException("failed to update tasks for TaskSet", exc);
        }

    }
}
