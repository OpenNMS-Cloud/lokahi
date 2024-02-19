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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.grpc.snmp.contract.SnmpResponse;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.opennms.horizon.inventory.util.Clock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class MinionHeartbeatConsumerTest {
    @Mock(strictness = Mock.Strictness.LENIENT) // At times, strict just gets it wrong and is distracting
    private MinionRpcClient rpcClient;

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Mock
    private MonitoringSystemService monitoringSystemService;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private MonitoringLocationService locationService;

    @Mock
    private Clock mockClock;

    @InjectMocks
    private MinionHeartbeatConsumer target;

    private final String TEST_TENANT_ID = "test-tenant";
    private final Long TEST_LOCATION_ID = new Random().nextLong(1, Long.MAX_VALUE);
    private final String TEST_LOCATION_ID_TEXT = String.valueOf(TEST_LOCATION_ID);
    private final String TEST_SYSTEM_ID = "test-system123";

    private TenantLocationSpecificHeartbeatMessage heartbeat;

    private MonitoringLocation testLocation;
    private MonitoringLocationDTO testLocationDTO;
    private GatewayRpcResponseProto testRpcResponse;
    private GatewayRpcResponseProto testRpcResponseNoEcho;

    @BeforeEach
    void beforeTest() {
        heartbeat = TenantLocationSpecificHeartbeatMessage.newBuilder()
                .setTenantId(TEST_TENANT_ID)
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .setIdentity(Identity.newBuilder().setSystemId(TEST_SYSTEM_ID).build())
                .build();

        EchoResponse response =
                EchoResponse.newBuilder().setTime(System.nanoTime()).build();
        testRpcResponse = GatewayRpcResponseProto.newBuilder()
                .setPayload(Any.pack(response))
                .build();

        SnmpResponse snmpResponse = SnmpResponse.newBuilder().build();
        testRpcResponseNoEcho = GatewayRpcResponseProto.newBuilder()
                .setPayload(Any.pack(snmpResponse))
                .build();

        ReflectionTestUtils.setField(target, "kafkaTopic", "test-topic");

        testLocation = new MonitoringLocation();
        testLocation.setLocation(TEST_LOCATION_ID_TEXT);
        testLocation.setTenantId(TEST_TENANT_ID);

        testLocationDTO =
                MonitoringLocationDTO.newBuilder().setId(TEST_LOCATION_ID).build();
    }

    @Test
    void testAcceptHeartbeats() throws LocationNotFoundException {
        doReturn(Optional.of(testLocation)).when(locationService).getByIdAndTenantId(TEST_LOCATION_ID, TEST_TENANT_ID);
        doReturn(CompletableFuture.completedFuture(testRpcResponse))
                .when(rpcClient)
                .sendRpcRequest(eq(TEST_TENANT_ID), any(GatewayRpcRequestProto.class));

        target.receiveMessage(heartbeat.toByteArray());
        verify(monitoringSystemService, timeout(5000).times(1))
                .addMonitoringSystemFromHeartbeat(any(TenantLocationSpecificHeartbeatMessage.class));
        verify(rpcClient, timeout(5000).atLeast(1))
                .sendRpcRequest(eq(TEST_TENANT_ID), any(GatewayRpcRequestProto.class));
        verify(kafkaTemplate, timeout(5000).atLeast(1)).send(any(ProducerRecord.class));
    }

    @Test
    void testAcceptHeartbeatsDelay() throws LocationNotFoundException {
        doReturn(Optional.of(testLocation)).when(locationService).getByIdAndTenantId(TEST_LOCATION_ID, TEST_TENANT_ID);
        doReturn(CompletableFuture.completedFuture(testRpcResponse))
                .when(rpcClient)
                .sendRpcRequest(eq(TEST_TENANT_ID), any(GatewayRpcRequestProto.class));
        when(mockClock.getCurrentTimeMs()).thenReturn(0L, 30_000L);

        target.receiveMessage(heartbeat.toByteArray());
        target.receiveMessage(heartbeat.toByteArray());
        verify(monitoringSystemService, timeout(5000).times(2))
                .addMonitoringSystemFromHeartbeat(any(TenantLocationSpecificHeartbeatMessage.class));
        verify(rpcClient, timeout(5000).times(2)).sendRpcRequest(eq(TEST_TENANT_ID), any(GatewayRpcRequestProto.class));
        verify(kafkaTemplate, timeout(5000).times(2)).send(any(ProducerRecord.class));
    }

    @Test
    void testOtherException() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        RuntimeException testException = new RuntimeException("x-test-exc-x");
        when(locationService.getByIdAndTenantId(TEST_LOCATION_ID, TEST_TENANT_ID))
                .thenReturn(Optional.of(testLocationDTO));
        doThrow(testException)
                .when(monitoringSystemService)
                .addMonitoringSystemFromHeartbeat(any(TenantLocationSpecificHeartbeatMessage.class));

        //
        // Execute
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(MinionHeartbeatConsumer.class)) {
            TenantLocationSpecificHeartbeatMessage message =
                    TenantLocationSpecificHeartbeatMessage.parseFrom(heartbeat.toByteArray());
            target.processHeartbeat(message);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                    (logEvent) -> ((Objects.equals("Error while processing heartbeat message: ", logEvent.getMessage()))
                            && (logEvent.getArguments().size() == 0)
                            && (logEvent.getThrowable().orElse(null) == testException));

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testRpcResponseNotEcho() {
        //
        // Setup Test Data and Interactions
        //
        when(locationService.getByIdAndTenantId(TEST_LOCATION_ID, TEST_TENANT_ID))
                .thenReturn(Optional.of(testLocationDTO));
        doReturn(CompletableFuture.completedFuture(testRpcResponseNoEcho))
                .when(rpcClient)
                .sendRpcRequest(eq(TEST_TENANT_ID), any(GatewayRpcRequestProto.class));
        target.setRpcMonitorRunner(
                (runnable) -> runnable.run()); // Immediately run to prevent asynchronous race conditions in the test

        //
        // Execute
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(MinionHeartbeatConsumer.class)) {
            TenantLocationSpecificHeartbeatMessage message =
                    TenantLocationSpecificHeartbeatMessage.parseFrom(heartbeat.toByteArray());
            target.processHeartbeat(message);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher = (logEvent) -> ((Objects.equals(
                            "Unable to complete echo request for monitoring with tenantId={}; locationId={}; systemId={}",
                            logEvent.getMessage()))
                    && (logEvent.getArguments().size() == 3)
                    && (Objects.equals(TEST_SYSTEM_ID, logEvent.getArguments().get(2)))
                    && (Objects.equals(
                            TEST_LOCATION_ID_TEXT, logEvent.getArguments().get(1)))
                    && (Objects.equals(TEST_TENANT_ID, logEvent.getArguments().get(0))));

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        } catch (InvalidProtocolBufferException e) {

        }
    }

    @Test
    void testShutdown() {
        //
        // Execute
        //
        target.shutDown();

        //
        // Verify the Results
        //
        verify(rpcClient).shutdown();
    }

    @Test
    void testShutdownNullRpcClient() {
        //
        // Setup Test Data and Interactions
        //
        MinionHeartbeatConsumer targetNullRpcClient = new MinionHeartbeatConsumer(null, null, null, null, null);

        //
        // Execute
        //
        target.shutDown();

        //
        // Verify the Results
        //

        // No explicit to verify - a lack of NPE is all that is needed
    }
}
