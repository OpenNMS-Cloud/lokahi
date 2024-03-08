/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;

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
        var testLocationList = List.of(
                MonitoringLocationDTO.newBuilder()
                        .setTenantId(TEST_TENANT_ID)
                        .setId(TEST_LOCATION_ID1)
                        .build(),
                MonitoringLocationDTO.newBuilder()
                        .setTenantId(TEST_TENANT_ID)
                        .setId(TEST_LOCATION_ID2)
                        .build());

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
                        Mockito.argThat(argument -> (argument.size() == 1)
                                && (argument.get(0)
                                        .getId()
                                        .equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID1))));
        Mockito.verify(mockTaskSetPublisher)
                .publishNewTasks(
                        Mockito.eq(TEST_TENANT_ID),
                        Mockito.eq(TEST_LOCATION_ID2),
                        Mockito.argThat(argument -> (argument.size() == 1)
                                && (argument.get(0)
                                        .getId()
                                        .equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID2))));
    }

    @Test
    void testExceptionOnSendFlowsConfigForOneLocationOutOfTwo() {
        //
        // Setup Test Data and Interactions
        //
        var testLocationList = List.of(
                MonitoringLocationDTO.newBuilder()
                        .setTenantId(TEST_TENANT_ID)
                        .setId(TEST_LOCATION_ID1)
                        .setLocation(TEST_LOCATION_ID1_NAME)
                        .build(),
                MonitoringLocationDTO.newBuilder()
                        .setTenantId(TEST_TENANT_ID)
                        .setId(TEST_LOCATION_ID2)
                        .setLocation(TEST_LOCATION_ID2_NAME)
                        .build());
        Mockito.when(mockMonitoringLocationService.findAll()).thenReturn(testLocationList);

        var testException = new RuntimeException("x-test-exception-x");
        Mockito.doThrow(testException)
                .when(mockTaskSetPublisher)
                .publishNewTasks(Mockito.anyString(), Mockito.eq(TEST_LOCATION_ID1), Mockito.any(List.class));

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
                    location -> Objects.equals(TEST_LOCATION_ID1_NAME, location));

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            assertEquals(1, logCaptor.getLogEvents().size());

            Mockito.verify(mockTaskSetPublisher)
                    .publishNewTasks(
                            Mockito.eq(TEST_TENANT_ID),
                            Mockito.eq(TEST_LOCATION_ID2),
                            Mockito.argThat(argument -> (argument.size() == 1)
                                    && (argument.get(0)
                                            .getId()
                                            .equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID2))));
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
                        Mockito.argThat(argument -> (argument.size() == 1)
                                && (argument.get(0)
                                        .getId()
                                        .equals(FlowsConfigService.FLOWS_CONFIG + "@" + TEST_LOCATION_ID1))));
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private Predicate<LogEvent> createLogEventMatcher(
            String logString, Exception expectedException, Predicate<Object>... expectedArgMatchers) {
        Predicate<LogEvent> matcher = (logEvent) -> ((Objects.equals(logString, logEvent.getMessage()))
                && (argumentsMatch(logEvent, expectedArgMatchers))
                && (logEvent.getThrowable().orElse(null) == expectedException));

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
