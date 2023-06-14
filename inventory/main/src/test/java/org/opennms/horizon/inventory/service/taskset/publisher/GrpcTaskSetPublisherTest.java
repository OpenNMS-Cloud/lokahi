/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertSame;

/**
 * Verify operation of the GrpcTaskSetPublisher.
 */
public class GrpcTaskSetPublisherTest {

    public static final int DEADLINE = 5000;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(GrpcTaskSetPublisherTest.class);

    private Logger LOG = DEFAULT_LOGGER;


    //
    // Target of the test
    //
    private GrpcTaskSetPublisher target;

    //
    // Test Mocks
    //
    private ManagedChannel mockManagedChannel;
    private TaskSetServiceGrpc.TaskSetServiceBlockingStub mockTaskSetServiceBlockingStub;

    //
    // Test Data
    //
    private List<TaskDefinition> testTaskDefinitionList1;
    private UpdateTasksResponse testUpdateTasksResponse;

    /**
     * Prepare common test data and interactions.
     */
    @BeforeEach
    void setUp() {
        mockManagedChannel = Mockito.mock(ManagedChannel.class);
        mockTaskSetServiceBlockingStub = Mockito.mock(TaskSetServiceGrpc.TaskSetServiceBlockingStub.class);

        TaskDefinition testTaskDefinition1 = TaskDefinition.newBuilder().setId("x-task-001-x").build();
        TaskDefinition testTaskDefinition2 = TaskDefinition.newBuilder().setId("x-task-002-x").build();

        testTaskDefinitionList1 = Arrays.asList(testTaskDefinition1, testTaskDefinition2);
        testUpdateTasksResponse = UpdateTasksResponse.newBuilder().build();

        target = new GrpcTaskSetPublisher(mockManagedChannel, DEADLINE);

        //
        // COMMON TEST INTERACTIONS (remove from here if not all tests agree on these interactions)
        //
        Mockito.when(mockTaskSetServiceBlockingStub.withInterceptors(Mockito.any(ClientInterceptor.class))).thenReturn(mockTaskSetServiceBlockingStub);
        Mockito.when(mockTaskSetServiceBlockingStub.withDeadlineAfter(DEADLINE, TimeUnit.MILLISECONDS)).thenReturn(mockTaskSetServiceBlockingStub);
        Mockito.when((mockTaskSetServiceBlockingStub).updateTasks(Mockito.any())).thenReturn(testUpdateTasksResponse);
    }

    /**
     * Test addition of new tasks.
     *
     * @throws Exception
     */
    @Test
    public void testAddTasks() throws Exception {
        //
        // EXECUTE
        //
        target.setTaskSetServiceBlockingStubSupplier(this::getTaskSetServiceBlockingStubForChannel);
        target.init();

        target.publishNewTasks("x-tenant-id-001-x", "x-location-x", testTaskDefinitionList1);

        //
        // VALIDATE
        //
        Mockito.verify(mockTaskSetServiceBlockingStub).withDeadlineAfter(DEADLINE, TimeUnit.MILLISECONDS);
        var matcher = createUpdateTaskRequestMatcher("x-tenant-id-001-x", testTaskDefinitionList1, TASK_OP.ADD);
        Mockito.verify(mockTaskSetServiceBlockingStub).updateTasks(Mockito.argThat(matcher));
    }

    /**
     * Test addition of new tasks on a second tenant.
     *
     * @throws Exception
     */
    @Test
    public void testAddTasksTenant2() throws Exception {
        //
        // EXECUTE
        //
        target.setTaskSetServiceBlockingStubSupplier(this::getTaskSetServiceBlockingStubForChannel);
        target.init();

        target.publishNewTasks("x-tenant-id-002-x", "x-location-x", testTaskDefinitionList1);

        //
        // VALIDATE
        //
        var matcher = createUpdateTaskRequestMatcher("x-tenant-id-002-x", testTaskDefinitionList1, TASK_OP.ADD);
        Mockito.verify(mockTaskSetServiceBlockingStub).updateTasks(Mockito.argThat(matcher));
    }

    /**
     * Test removal of tasks.
     *
     * @throws Exception
     */
    @Test
    public void testRemoveTasks() throws Exception {
        //
        // EXECUTE
        //
        target.setTaskSetServiceBlockingStubSupplier(this::getTaskSetServiceBlockingStubForChannel);
        target.init();

        target.publishTaskDeletion("x-tenant-id-001-x", "x-location-x", testTaskDefinitionList1);

        //
        // VALIDATE
        //
        Mockito.verify(mockTaskSetServiceBlockingStub).withDeadlineAfter(DEADLINE, TimeUnit.MILLISECONDS);
        var matcher = createUpdateTaskRequestMatcher("x-tenant-id-001-x", testTaskDefinitionList1, TASK_OP.REMOVE);
        Mockito.verify(mockTaskSetServiceBlockingStub).updateTasks(Mockito.argThat(matcher));
    }


//========================================
// Internals
//----------------------------------------

    private TaskSetServiceGrpc.TaskSetServiceBlockingStub getTaskSetServiceBlockingStubForChannel(ManagedChannel channel) {
        assertSame(mockManagedChannel, channel);

        return mockTaskSetServiceBlockingStub;
    }

    /**
     * Create an argument matcher for the TaskSetServiceBlockingStub's updateTasks(...) call given a list of expected
     * tasks and the type of operation being performed on those tasks (addition vs removal).
     *
     * @param tenantId
     * @param expectedTasks list of task definitions being added to, or removed from, the task set.
     * @param taskOp        type of operation being performed on the task set.
     * @return argument matcher for use in Mockito verifications.
     */
    private ArgumentMatcher<UpdateTasksRequest> createUpdateTaskRequestMatcher(String tenantId, List<TaskDefinition> expectedTasks, TASK_OP taskOp) {
        return new ArgumentMatcher<UpdateTasksRequest>() {
            @Override
            public boolean matches(UpdateTasksRequest argument) {
                if (Objects.equals(argument.getTenantId(), tenantId)) {
                    if (argument.getUpdateCount() == expectedTasks.size()) {
                        int cur = 0;

                        for (var oneUpdate : argument.getUpdateList()) {
                            if (taskOp == TASK_OP.ADD) {
                                // ADD
                                if (oneUpdate.hasAddTask()) {
                                    var addTask = oneUpdate.getAddTask();
                                    var expectedAddTask = expectedTasks.get(cur);

                                    if (!Objects.equals(expectedAddTask, addTask.getTaskDefinition())) {
                                        LOG.error("Add task mismatch: offset={}; expected={}; actual={}", cur, expectedAddTask, addTask.getTaskDefinition());
                                        return false;
                                    }

                                    LOG.debug("MATCHED: cur={}; expected={}; actual={}", cur, expectedAddTask, addTask.getTaskDefinition());
                                } else {
                                    LOG.error("expecting Add Task in update");
                                }
                            } else if (taskOp == TASK_OP.REMOVE) {
                                // REMOVE
                                if (oneUpdate.hasRemoveTask()) {
                                    var removeTask = oneUpdate.getRemoveTask();
                                    var expectedRemoveTask = expectedTasks.get(cur);

                                    if (!Objects.equals(expectedRemoveTask.getId(), removeTask.getTaskId())) {
                                        LOG.error("Remove task mismatch: offset={}; expected={}; actual={}", cur, expectedRemoveTask.getId(), removeTask.getTaskId());
                                        return false;
                                    }

                                    LOG.debug("MATCHED: cur={}; expected={}; actual={}", cur, expectedRemoveTask.getId(), removeTask.getTaskId());
                                } else {
                                    LOG.error("expecting Remove Task in update");
                                }
                            } else {
                                throw new RuntimeException("Unrecognized task operation type " + taskOp);
                            }

                            cur++;
                        }

                        // SUCCESS
                        return true;
                    }
                } else {
                    LOG.debug("Tenant ID mismatch: expected={}; actual={}", tenantId, argument.getTenantId());
                }

                return false;
            }
        };
    }

    private enum TASK_OP {
        ADD,
        REMOVE
    }
}
