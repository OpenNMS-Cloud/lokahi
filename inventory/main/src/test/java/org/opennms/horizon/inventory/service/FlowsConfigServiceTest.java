/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlowsConfigServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";
    public static final long TEST_LOCATION_ID1 = 1313L;
    public static final long TEST_LOCATION_ID2 = 1717L;
    public static final String TEST_LOCATION_ID1_NAME = "x-loc-001-x";
    public static final String TEST_LOCATION_ID2_NAME = "x-loc-002-x";

    private MonitoringLocationService mockMonitoringLocationService;
    private TaskSetPublisher mockTaskSetPublisher;

    private FlowsConfigService target;

    @BeforeEach
    public void setUp() {
        mockMonitoringLocationService = Mockito.mock(MonitoringLocationService.class);
        mockTaskSetPublisher = Mockito.mock(TaskSetPublisher.class);

        target = new FlowsConfigService(mockMonitoringLocationService, mockTaskSetPublisher);
    }

    @Test
    void testSendFlowConfigToMinionAfterStartup() {
        //
        // Setup Test Data and Interactions
        //
        var testLocationList =
            List.of(
                MonitoringLocationDTO.newBuilder().setTenantId(TEST_TENANT_ID).setId(TEST_LOCATION_ID1).build(),
                MonitoringLocationDTO.newBuilder().setTenantId(TEST_TENANT_ID).setId(TEST_LOCATION_ID2).build()
            );

        Mockito.when(mockMonitoringLocationService.findAll()).thenReturn(testLocationList);

        //
        // Execute
        //
        target.sendFlowConfigToMinionAfterStartup();

        //
        // Verify the Results
        //
        Mockito.verify(mockTaskSetPublisher)
            .publishNewTasks(
                Mockito.eq(TEST_TENANT_ID),
                Mockito.eq(TEST_LOCATION_ID1),
                Mockito.argThat(
                    argument -> ( argument.size() == 1 ) && ( argument.get(0).getId().equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID1) )
                )
            );
        Mockito.verify(mockTaskSetPublisher)
            .publishNewTasks(
                Mockito.eq(TEST_TENANT_ID),
                Mockito.eq(TEST_LOCATION_ID2),
                Mockito.argThat(
                    argument -> ( argument.size() == 1 ) && ( argument.get(0).getId().equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID2) )
                )
            );
    }

    @Test
    void testExceptionOnSendFlowsConfigForOneLocationOutOfTwo() {
        //
        // Setup Test Data and Interactions
        //
        var testLocationList =
            List.of(
                MonitoringLocationDTO.newBuilder().setTenantId(TEST_TENANT_ID).setId(TEST_LOCATION_ID1).setLocation(TEST_LOCATION_ID1_NAME).build(),
                MonitoringLocationDTO.newBuilder().setTenantId(TEST_TENANT_ID).setId(TEST_LOCATION_ID2).setLocation(TEST_LOCATION_ID2_NAME).build()
            );
        Mockito.when(mockMonitoringLocationService.findAll()).thenReturn(testLocationList);

        var testException = new RuntimeException("x-test-exception-x");
        Mockito.doThrow(testException).when(mockTaskSetPublisher)
            .publishNewTasks(
                Mockito.anyString(),
                Mockito.eq(TEST_LOCATION_ID1),
                Mockito.any(List.class));

        //
        // Execute
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(FlowsConfigService.class)) {
            target.sendFlowConfigToMinionAfterStartup();

            //
            // Verify the Results
            //
            var matcher = createLogEventMatcher(
                "Failed to send flow config: tenant={}; location={}",
                testException,
                tenant -> Objects.equals(TEST_TENANT_ID, tenant),
                location -> Objects.equals(TEST_LOCATION_ID1_NAME, location)
            );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            assertEquals(1, logCaptor.getLogEvents().size());

            Mockito.verify(mockTaskSetPublisher)
                .publishNewTasks(
                    Mockito.eq(TEST_TENANT_ID),
                    Mockito.eq(TEST_LOCATION_ID2),
                    Mockito.argThat(
                        argument -> ( argument.size() == 1 ) && ( argument.get(0).getId().equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID2) )
                    )
                );
        }
    }

    @Test
    void testSendFlowsConfigToMinion() {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.sendFlowsConfigToMinion(TEST_TENANT_ID, TEST_LOCATION_ID1);

        //
        // Verify the Results
        //
        Mockito.verify(mockTaskSetPublisher)
            .publishNewTasks(
                Mockito.eq(TEST_TENANT_ID),
                Mockito.eq(TEST_LOCATION_ID1),
                Mockito.argThat(
                    argument -> ( argument.size() == 1 ) && ( argument.get(0).getId().equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID1) )
                )
            );
    }

//========================================
// Internals
//----------------------------------------

    private Predicate<LogEvent> createLogEventMatcher(String logString, Exception expectedException, Predicate<Object>... expectedArgMatchers) {
        Predicate<LogEvent> matcher =
            (logEvent) ->
                (
                    ( Objects.equals(logString, logEvent.getMessage()) ) &&
                    ( argumentsMatch(logEvent, expectedArgMatchers) ) &&
                    ( logEvent.getThrowable().orElse(null) == expectedException )
                );

        return matcher;
    }

    private boolean argumentsMatch(LogEvent logEvent, Predicate<Object>... expectedArgMatchers) {
        if (logEvent.getArguments().size() != expectedArgMatchers.length) {
            return false;
        }

        int cur = 0;
        while (cur < expectedArgMatchers.length) {
            if (!expectedArgMatchers[cur].test(logEvent.getArguments().get(cur))) {
                return false;
            }
            cur++;
        }

        return true;
    }
}
