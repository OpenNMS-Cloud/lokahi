package org.opennms.miniongateway.taskset.service;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.RemoveSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;

import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class TaskSetGrpcServiceUpdateProcessorTest {

    private TaskDefinition testTaskDefinition1;
    private TaskDefinition testTaskDefinition2;
    private AddSingleTaskOp testAddSingleTaskOp1;
    private AddSingleTaskOp testAddSingleTaskOp2;
    private RemoveSingleTaskOp testRemoveSingleTaskOp;
    private UpdateTasksRequest testRequestAdd1Remove1;
    private UpdateTasksRequest testRequestRemove1;
    private UpdateTasksRequest testRequestAdd1;
    private UpdateTasksRequest testRequestAddAnother1;
    private UpdateTasksRequest testRequestNeitherAddNorRemove;
    private TaskDefinition testRemovalTaskDefinition;

    @BeforeEach
    void setUp() {
        testTaskDefinition1 =
            TaskDefinition.newBuilder()
                .setId("x-task-001-id-x")
                .setPluginName("x-plugin-name-x")
                .build();

        testTaskDefinition2 =
            TaskDefinition.newBuilder()
                .setId("x-task-002-id-x")
                .setPluginName("x-plugin-name-x")
                .build();

        testRemovalTaskDefinition =
            TaskDefinition.newBuilder()
                .setId("x-task-002-id-x")
                .build();

        testAddSingleTaskOp1 = AddSingleTaskOp.newBuilder().setTaskDefinition(testTaskDefinition1).build();

        testAddSingleTaskOp2 = AddSingleTaskOp.newBuilder().setTaskDefinition(testTaskDefinition2).build();

        testRemoveSingleTaskOp =
            RemoveSingleTaskOp.newBuilder()
                .setTaskId("x-task-002-id-x")
                .build();

        testRequestAdd1Remove1 =
            UpdateTasksRequest.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .addUpdate(UpdateSingleTaskOp.newBuilder().setAddTask(testAddSingleTaskOp1).build())
                .addUpdate(UpdateSingleTaskOp.newBuilder().setRemoveTask(testRemoveSingleTaskOp).build())
                .build();

        testRequestRemove1 =
            UpdateTasksRequest.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .addUpdate(UpdateSingleTaskOp.newBuilder().setRemoveTask(testRemoveSingleTaskOp).build())
                .build();

        testRequestAdd1 =
            UpdateTasksRequest.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .addUpdate(UpdateSingleTaskOp.newBuilder().setAddTask(testAddSingleTaskOp1).build())
                .build();
        
        testRequestNeitherAddNorRemove =
            UpdateTasksRequest.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .addUpdate(UpdateSingleTaskOp.newBuilder().build())
                .build();

        testRequestAddAnother1 =
            UpdateTasksRequest.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .addUpdate(UpdateSingleTaskOp.newBuilder().setAddTask(testAddSingleTaskOp2).build())
                .build();
    }

    @Test
    void testAdd1Remove1FromEmpty() {
        //
        // Setup Test Data and Interactions
        //
        TaskSet testEmptyOriginal = TaskSet.newBuilder().build();
        var target = new TaskSetGrpcServiceUpdateProcessor(testRequestAdd1Remove1);

        //
        // Execute
        //
        TaskSet result = target.process(testEmptyOriginal);

        //
        // Verify the Results
        //
        assertEquals(1, result.getTaskDefinitionCount());
        assertSame(testTaskDefinition1, result.getTaskDefinition(0));
        assertEquals(1, target.getNumNew());
        assertEquals(0, target.getNumRemoved());
        assertEquals(0, target.getNumReplaced());
    }

    @Test
    void testAdd1Remove1BothExist() {
        //
        // Setup Test Data and Interactions
        //
        TaskSet testOriginal =
            TaskSet.newBuilder()
                .addTaskDefinition(testTaskDefinition1)
                .addTaskDefinition(testRemovalTaskDefinition)
                .build();
        var target = new TaskSetGrpcServiceUpdateProcessor(testRequestAdd1Remove1);

        //
        // Execute
        //
        TaskSet result = target.process(testOriginal);

        //
        // Verify the Results
        //
        assertEquals(1, result.getTaskDefinitionCount());
        assertSame(testTaskDefinition1, result.getTaskDefinition(0));
        assertEquals(0, target.getNumNew());
        assertEquals(1, target.getNumRemoved());
        assertEquals(1, target.getNumReplaced());
    }

    @Test
    void testRemove1FromEmpty() {
        //
        // Setup Test Data and Interactions
        //
        TaskSet testEmptyOriginal = TaskSet.newBuilder().build();
        var target = new TaskSetGrpcServiceUpdateProcessor(testRequestRemove1);

        //
        // Execute
        //
        TaskSet result = target.process(testEmptyOriginal);

        //
        // Verify the Results
        //
        assertEquals(0, result.getTaskDefinitionCount());
        assertEquals(0, target.getNumNew());
        assertEquals(0, target.getNumRemoved());
        assertEquals(0, target.getNumReplaced());
    }

    @Test
    void testReplaceExisting() {
        //
        // Setup Test Data and Interactions
        //
        TaskSet testOriginalWithTaskThatWillBeReplaced =
            TaskSet.newBuilder()
                .addTaskDefinition(testTaskDefinition1)
                .build();
        var target = new TaskSetGrpcServiceUpdateProcessor(testRequestAdd1);

        //
        // Execute
        //
        TaskSet result = target.process(testOriginalWithTaskThatWillBeReplaced);

        //
        // Verify the Results
        //
        assertEquals(1, result.getTaskDefinitionCount());
        assertEquals(0, target.getNumNew());
        assertEquals(0, target.getNumRemoved());
        assertEquals(1, target.getNumReplaced());
    }

    @Test
    void testRequestIsNeitherAddNorRemove() {
        //
        // Setup Test Data and Interactions
        //
        TaskSet testEmptyOriginal = TaskSet.newBuilder().build();
        var target = new TaskSetGrpcServiceUpdateProcessor(testRequestNeitherAddNorRemove);


        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetGrpcServiceUpdateProcessor.class)) {
            //
            // Execute
            //
            TaskSet result = target.process(testEmptyOriginal);

            //
            // Verify the Results
            //

            /* TBD888
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Ignoring unrecognized update request with no add-task and no remove-task: tenant-id={}; location={}", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 2) &&
                        (logEvent.getArguments().get(0).equals("x-tenant-id-x")) &&
                        (logEvent.getArguments().get(1).equals("x-location-x"))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            */
            assertEquals(0, result.getTaskDefinitionCount());
            assertEquals(0, target.getNumNew());
            assertEquals(0, target.getNumRemoved());
            assertEquals(0, target.getNumReplaced());
        }
    }

    @Test
    void testRequestAddSecondTask() {
        //
        // Setup Test Data and Interactions
        //
        TaskSet testOriginalWith1Task =
            TaskSet.newBuilder()
                .addTaskDefinition(testTaskDefinition1)
                .build();
        var target = new TaskSetGrpcServiceUpdateProcessor(testRequestAddAnother1);

        //
        // Execute
        //
        TaskSet result = target.process(testOriginalWith1Task);

        //
        // Verify the Results
        //

        assertEquals(2, result.getTaskDefinitionCount());
        assertEquals(1, target.getNumNew());
        assertEquals(0, target.getNumRemoved());
        assertEquals(0, target.getNumReplaced());
    }
}
