package org.opennms.horizon.inventory.service;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

public class ConfigUpdateServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";
    public static final long TEST_LOCATION_ID = 1313L;
    public static final String TEST_LOCATION_TEXT = String.valueOf(TEST_LOCATION_ID);

    private TrapConfigService mockTrapConfigService;
    private FlowsConfigService mockFlowsConfigService;
    private TaskSetPublisher mockTaskSetPublisher;
    private ExecutorService mockExecutorService;

    private ConfigUpdateService target;

    @BeforeEach
    public void setUp() {
        mockTrapConfigService = Mockito.mock(TrapConfigService.class);
        mockFlowsConfigService = Mockito.mock(FlowsConfigService.class);
        mockTaskSetPublisher = Mockito.mock(TaskSetPublisher.class);
        mockExecutorService = Mockito.mock(ExecutorService.class);

        Mockito.doAnswer(this::runImmediatelyInline).when(mockExecutorService).execute(Mockito.any(Runnable.class));

        target = new ConfigUpdateService(mockTrapConfigService, mockFlowsConfigService, mockTaskSetPublisher);
        target.setExecutorService(mockExecutorService);
    }

    @Test
    void testSendConfigUpdate() {
        //
        // Execute
        //
        target.sendConfigUpdate(TEST_TENANT_ID, TEST_LOCATION_ID);

        //
        // Verify the Results
        //
        Mockito.verify(mockTrapConfigService).sendTrapConfigToMinion(TEST_TENANT_ID, TEST_LOCATION_ID);
        Mockito.verify(mockFlowsConfigService).sendFlowsConfigToMinion(TEST_TENANT_ID, TEST_LOCATION_ID);
    }

    @Test
    void testExceptionsOnSendConfigUpdate() {
        //
        // Setup Test Data and Interactions
        //
        var testException1 = new RuntimeException("x-test-exception1-x");
        var testException2 = new RuntimeException("x-test-exception2-x");
        Mockito.doThrow(testException1).when(mockTrapConfigService).sendTrapConfigToMinion(TEST_TENANT_ID, TEST_LOCATION_ID);
        Mockito.doThrow(testException2).when(mockFlowsConfigService).sendFlowsConfigToMinion(TEST_TENANT_ID, TEST_LOCATION_ID);

        //
        // Execute
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(ConfigUpdateService.class)) {
            target.sendConfigUpdate(TEST_TENANT_ID, TEST_LOCATION_ID);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher1 =
                (logEvent) -> (
                        Objects.equals("Exception while sending traps to Minion", logEvent.getMessage()) &&
                        (logEvent.getArguments().size() == 0) &&
                        ( logEvent.getThrowable().orElse(null) == testException1 )
                    );
            Assertions.assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher1));

            Predicate<LogEvent> matcher2 =
                (logEvent) -> (
                        Objects.equals("Exception while sending flows to Minion", logEvent.getMessage()) &&
                        (logEvent.getArguments().size() == 0) &&
                        ( logEvent.getThrowable().orElse(null) == testException2 )
                    );
            Assertions.assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher2));
        }
    }

    @Test
    void testRemoveConfigsFromTaskSet() {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.removeConfigsFromTaskSet(TEST_TENANT_ID, TEST_LOCATION_ID);

        //
        // Verify the Results
        //
        Mockito.verify(mockTaskSetPublisher)
            .publishTaskDeletion(
                Mockito.eq(TEST_TENANT_ID),
                Mockito.eq(TEST_LOCATION_ID),
                Mockito.argThat(
                    argument ->
                        (
                            (argument.size() == 2) &&
                            (Objects.equals(argument.get(0).getId(), TrapConfigService.TRAPS_CONFIG + "@" + TEST_LOCATION_TEXT)) &&
                            (Objects.equals(argument.get(1).getId(), FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_TEXT))
                        )
                )
            );
    }

//========================================
// Internals
//----------------------------------------

    private Object runImmediatelyInline(InvocationOnMock invocation) {
        Runnable runnable = invocation.getArgument(0);
        runnable.run();
        return null;
    }
}
