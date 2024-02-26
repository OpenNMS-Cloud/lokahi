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
package org.opennms.horizon.inventory.component;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventParameter;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceState;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceStateRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.service.taskset.response.MonitorResponseService;
import org.opennms.horizon.inventory.service.taskset.response.ScannerResponseService;
import org.opennms.horizon.shared.events.EventConstants;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;

public class TaskSetResultsConsumerTest {

    public static final long TEST_LOCATION_ID = 1313L;
    public static final String TEST_LOCATION_ID_TEXT = String.valueOf(TEST_LOCATION_ID);

    private TaskSetResultsConsumer target;

    private ScannerResponseService mockScannerResponseService;

    private MonitorResponseService monitorResponseService;

    private MonitoredServiceRepository mockMonitoredServiceRepository;

    private MonitoredServiceStateRepository mockMonitoredServiceStateRepository;

    private MonitoringLocationRepository mockMonitoringLocationRepository;

    private InternalEventProducer mockEventProducer;

    @BeforeEach
    public void setUp() {

        mockScannerResponseService = Mockito.mock(ScannerResponseService.class);

        mockMonitoredServiceStateRepository = Mockito.mock(MonitoredServiceStateRepository.class);
        mockMonitoredServiceRepository = Mockito.mock(MonitoredServiceRepository.class);
        mockMonitoringLocationRepository = Mockito.mock(MonitoringLocationRepository.class);
        mockEventProducer = Mockito.mock(InternalEventProducer.class);

        monitorResponseService = Mockito.spy(new MonitorResponseService(
                mockMonitoredServiceStateRepository,
                mockMonitoredServiceRepository,
                mockMonitoringLocationRepository,
                mockEventProducer));

        target = new TaskSetResultsConsumer(mockScannerResponseService, monitorResponseService);
    }

    @Test
    void testReceiveZeroResults() {
        //
        // Setup Test Data and Interactions
        //
        var testTenantLocationSpecificTaskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        await().during(3, TimeUnit.SECONDS).until(() -> {
            Mockito.verify(mockScannerResponseService, Mockito.never())
                    .accept(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
            return true;
        });
    }

    @Test
    void testReceiveOneScannerResult() throws InvalidProtocolBufferException {
        //
        // Setup Test Data and Interactions
        //
        var testScannerResponse = ScannerResponse.newBuilder().build();

        var testResult = makeScannerTaskResult("x-task-id-001-x", testScannerResponse);

        var testTenantLocationSpecificTaskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(testResult)
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        Mockito.verify(mockScannerResponseService, Mockito.timeout(3000))
                .accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse);
    }

    @Test
    void testReceiveOneNonScannerResult() throws InvalidProtocolBufferException {
        //
        // Setup Test Data and Interactions
        //
        var testCollectorResponse = CollectorResponse.newBuilder().build();

        var testResult = TaskResult.newBuilder()
                .setId("x-task-id-001-x")
                .setCollectorResponse(testCollectorResponse)
                .build();

        var testTenantLocationSpecificTaskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(testResult)
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        await().during(3, TimeUnit.SECONDS).until(() -> {
            Mockito.verify(mockScannerResponseService, Mockito.never())
                    .accept(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
            return true;
        });
    }

    @Test
    void testReceive3ScannerResults() throws InvalidProtocolBufferException {
        //
        // Setup Test Data and Interactions
        //
        var testScannerResponse1 =
                ScannerResponse.newBuilder().setReason("x-reason1-x").build();
        var testScannerResponse2 =
                ScannerResponse.newBuilder().setReason("x-reason2-x").build();
        var testScannerResponse3 =
                ScannerResponse.newBuilder().setReason("x-reason3-x").build();

        var testResult1 = makeScannerTaskResult("x-task-id-001-x", testScannerResponse1);
        var testResult2 = makeScannerTaskResult("x-task-id-002-x", testScannerResponse2);
        var testResult3 = makeScannerTaskResult("x-task-id-003-x", testScannerResponse3);

        var testTenantLocationSpecificTaskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(testResult1)
                .addResults(testResult2)
                .addResults(testResult3)
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        Mockito.verify(mockScannerResponseService, Mockito.timeout(3000))
                .accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse1);
        Mockito.verify(mockScannerResponseService, Mockito.timeout(3000))
                .accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse2);
        Mockito.verify(mockScannerResponseService, Mockito.timeout(3000))
                .accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse3);
        Mockito.verifyNoMoreInteractions(mockScannerResponseService);
    }

    @Test
    void testReceiveMonitorResults() {
        //
        // Setup Test Data and Interactions
        //
        String tenantId = "x-tenant-id-x";

        Mockito.when(mockMonitoredServiceRepository.findByIdAndTenantId(1, tenantId))
                .thenReturn(Optional.of(new MonitoredService()));
        Mockito.when(mockMonitoredServiceStateRepository.findByTenantIdAndMonitoredServiceId(tenantId, 1))
                .thenReturn(Optional.of(new MonitoredServiceState()));

        var monitorResponse1 = MonitorResponse.newBuilder()
                .setMonitorType(MonitorType.ICMP)
                .setMonitorServiceId(1)
                .setNodeId(1)
                .setStatus("Up")
                .setResponseTimeMs(1.0d)
                .build();
        var monitorResponse2 = MonitorResponse.newBuilder()
                .setMonitorType(MonitorType.ICMP)
                .setMonitorServiceId(1)
                .setNodeId(1)
                .setStatus("Down")
                .setReason("reason")
                .build();
        var monitorResponse3 = MonitorResponse.newBuilder()
                .setMonitorType(MonitorType.ICMP)
                .setMonitorServiceId(1)
                .setNodeId(1)
                .setStatus("Up")
                .setResponseTimeMs(1.1d)
                .build();

        var testResult1 = makeMonitorTaskResult("x-task-id-001-x", monitorResponse1);
        var testResult2 = makeMonitorTaskResult("x-task-id-002-x", monitorResponse2);
        var testResult3 = makeMonitorTaskResult("x-task-id-003-x", monitorResponse3);

        var testTenantLocationSpecificTaskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(testResult1)
                .addResults(testResult2)
                .addResults(testResult3)
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        Mockito.verify(monitorResponseService, Mockito.timeout(3000))
                .updateMonitoredState("x-tenant-id-x", TEST_LOCATION_ID_TEXT, monitorResponse1);
        Mockito.verify(monitorResponseService, Mockito.timeout(3000))
                .updateMonitoredState("x-tenant-id-x", TEST_LOCATION_ID_TEXT, monitorResponse2);
        Mockito.verify(monitorResponseService, Mockito.timeout(3000))
                .updateMonitoredState("x-tenant-id-x", TEST_LOCATION_ID_TEXT, monitorResponse3);

        var serviceName = EventParameter.newBuilder().setName("serviceName").setValue(MonitorType.ICMP.toString());
        var serviceId = EventParameter.newBuilder().setName("serviceId").setValue("1");

        Mockito.verify(mockEventProducer)
                .sendEvent(EventLog.newBuilder()
                        .setTenantId(tenantId)
                        .addEvents(Event.newBuilder()
                                .setTenantId(tenantId)
                                .setNodeId(1)
                                .setDescription("reason")
                                .setUei(EventConstants.SERVICE_UNREACHABLE_EVENT_UEI)
                                .setLocationId(TEST_LOCATION_ID_TEXT)
                                .addParameters(serviceName)
                                .addParameters(serviceId))
                        .build());

        Mockito.verify(mockEventProducer)
                .sendEvent(EventLog.newBuilder()
                        .setTenantId(tenantId)
                        .addEvents(Event.newBuilder()
                                .setTenantId(tenantId)
                                .setNodeId(1)
                                .setUei(EventConstants.SERVICE_RESTORED_EVENT_UEI)
                                .setLocationId(TEST_LOCATION_ID_TEXT)
                                .addParameters(serviceName)
                                .addParameters(serviceId))
                        .build());

        Mockito.verifyNoMoreInteractions(monitorResponseService);
    }

    @Test
    void testEmptyTenantId() {
        //
        // Setup Test Data and Interactions
        //
        var testScannerResponse = ScannerResponse.newBuilder().build();

        var testResult = makeScannerTaskResult("x-task-id-001-x", testScannerResponse);

        var testTenantLocationSpecificTaskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(testResult)
                .build();

        //
        // Execute
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetResultsConsumer.class)) {
            var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
            target.receiveMessage(messageBytes);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher = (logEvent) ->
                    (Objects.equals("Error while processing kafka message for TaskResults: ", logEvent.getMessage())
                            && (logEvent.getArguments().size() == 0)
                            && (logEvent.getThrowable().orElse(null) instanceof InventoryRuntimeException)
                            && (Objects.equals(
                                    "Missing tenant id",
                                    logEvent.getThrowable().orElse(null).getMessage())));

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            await().during(3, TimeUnit.SECONDS).until(() -> {
                Mockito.verify(mockScannerResponseService, Mockito.never())
                        .accept(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
                return true;
            });
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private TaskResult makeScannerTaskResult(String id, ScannerResponse scannerResponse) {
        return TaskResult.newBuilder()
                .setId(id)
                .setScannerResponse(scannerResponse)
                .build();
    }

    private TaskResult makeMonitorTaskResult(String id, MonitorResponse monitorResponse) {
        return TaskResult.newBuilder()
                .setId(id)
                .setMonitorResponse(monitorResponse)
                .build();
    }
}
