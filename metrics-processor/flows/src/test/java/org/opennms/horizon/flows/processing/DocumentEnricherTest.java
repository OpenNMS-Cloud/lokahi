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
package org.opennms.horizon.flows.processing;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.Descriptors;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.Locality;
import org.opennms.horizon.flows.document.NetflowVersion;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;

class DocumentEnricherTest {

    private InventoryClient mockInventoryClient;
    private ClassificationEngine mockClassificationEngine;
    private FlowDocumentClassificationRequestMapper mockFlowDocumentClassificationRequestMapper;
    private ClassificationRequest mockClassificationRequest;

    private TenantLocationSpecificFlowDocumentLog testDocumentLog;

    private DocumentEnricherImpl target;

    @BeforeEach
    public void setUp() {
        mockInventoryClient = Mockito.mock(InventoryClient.class);
        mockClassificationEngine = Mockito.mock(ClassificationEngine.class);
        mockFlowDocumentClassificationRequestMapper = Mockito.mock(FlowDocumentClassificationRequestMapper.class);
        mockClassificationRequest = Mockito.mock(ClassificationRequest.class);

        testDocumentLog = TenantLocationSpecificFlowDocumentLog.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .addMessage(FlowDocument.newBuilder()
                        .setSrcAddress("1.1.1.1")
                        .setSrcPort(UInt32Value.of(510))
                        .setDstAddress("2.2.2.2")
                        .setDstPort(UInt32Value.of(80))
                        .setProtocol(UInt32Value.of(6)) // TCP
                        .setNetflowVersion(NetflowVersion.V5)
                        .setExporterAddress("127.0.0.1"))
                .build();

        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 0);

        Mockito.when(mockFlowDocumentClassificationRequestMapper.createClassificationRequest(
                        Mockito.any(FlowDocument.class), Mockito.any(String.class)))
                .thenReturn(mockClassificationRequest);
    }

    @Test
    void testEnrich() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLog);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
    }

    @Test
    void testPositiveClockSkewThreshold() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);

        var now = Instant.now();
        int timeOffset = 100;

        var testDocumentLogWithTimestamps = TenantLocationSpecificFlowDocumentLog.newBuilder(testDocumentLog);
        testDocumentLogWithTimestamps
                .getMessageBuilder(0)
                .setReceivedAt(now.plus(timeOffset, ChronoUnit.MILLIS).toEpochMilli())
                .setTimestamp(now.toEpochMilli())
                .setFirstSwitched(
                        UInt64Value.of(now.minus(20_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setDeltaSwitched(
                        UInt64Value.of(now.minus(10_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setLastSwitched(
                        UInt64Value.of(now.minus(5_000L, ChronoUnit.MILLIS).toEpochMilli()));

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLogWithTimestamps.build());

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals("x-tenant-id-x", testDocumentLogWithTimestamps.getTenantId());
        assertEquals(100, result.get(0).getClockCorrection());
        assertEquals(
                now.plus(Duration.ofMillis(100)).toEpochMilli(),
                result.get(0).getTimestamp()); // plus because skew is -100
        assertEquals(
                now.minus(19_900L, ChronoUnit.MILLIS).toEpochMilli(),
                result.get(0).getFirstSwitched().getValue());
        assertEquals(
                now.minus(9_900L, ChronoUnit.MILLIS).toEpochMilli(),
                result.get(0).getDeltaSwitched().getValue());
        assertEquals(
                now.minus(4_900L, ChronoUnit.MILLIS).toEpochMilli(),
                result.get(0).getLastSwitched().getValue());
        assertEquals(Locality.PUBLIC, result.get(0).getSrcLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getDstLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getFlowLocality());

        verifySameExcluding(
                testDocumentLogWithTimestamps.getMessage(0),
                result.get(0),
                "first_switched",
                "delta_switched",
                "last_switched",
                "timestamp",
                "clock_correction",
                "src_locality",
                "dst_locality",
                "flow_locality");
    }

    @Test
    void testPositiveClockSkewThresholdMissedBy1() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);

        var now = Instant.now();
        int timeOffset = 99;

        var testDocumentLogWithTimestamps = TenantLocationSpecificFlowDocumentLog.newBuilder(testDocumentLog);
        testDocumentLogWithTimestamps
                .getMessageBuilder(0)
                .setReceivedAt(now.plus(timeOffset, ChronoUnit.MILLIS).toEpochMilli())
                .setTimestamp(now.toEpochMilli())
                .setFirstSwitched(
                        UInt64Value.of(now.minus(20_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setDeltaSwitched(
                        UInt64Value.of(now.minus(10_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setLastSwitched(
                        UInt64Value.of(now.minus(5_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .build();

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLogWithTimestamps.build());

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals("x-tenant-id-x", testDocumentLogWithTimestamps.getTenantId());
        assertEquals(0, result.get(0).getClockCorrection());
        assertEquals(Locality.PUBLIC, result.get(0).getSrcLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getDstLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getFlowLocality());

        verifySameExcluding(
                testDocumentLogWithTimestamps.getMessage(0),
                result.get(0),
                "src_locality",
                "dst_locality",
                "flow_locality");
    }

    @Test
    void testEnrichNone() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        var noneLog = TenantLocationSpecificFlowDocumentLog.newBuilder();

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(noneLog.build());

        //
        // Verify the Results
        //
        assertEquals(0, result.size());
    }

    @Test
    void testEnrichNodeLookupNotFoundException() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        StatusRuntimeException testException = new StatusRuntimeException(Status.NOT_FOUND);

        Mockito.when(mockInventoryClient.getIpInterfaceFromQuery("x-tenant-id-x", "1.1.1.1", "x-location-x"))
                .thenThrow(testException);

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLog);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertFalse(result.get(0).hasSrcNode());
        assertEquals(Locality.PUBLIC, result.get(0).getSrcLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getDstLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getFlowLocality());

        verifySameExcluding(
                testDocumentLog.getMessage(0), result.get(0), "src_locality", "dst_locality", "flow_locality");
    }

    @Test
    void testEnrichNodeLookupFound() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        IpInterfaceDTO testIpInterfaceDTO = IpInterfaceDTO.newBuilder()
                .setNodeId(123123)
                .setId(456456)
                .setHostname("x-hostname-x")
                .build();

        Mockito.when(mockInventoryClient.getIpInterfaceFromQuery("x-tenant-id-x", "1.1.1.1", "x-location-x"))
                .thenReturn(testIpInterfaceDTO);

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLog);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertTrue(result.get(0).hasSrcNode());
        assertEquals(123123, result.get(0).getSrcNode().getNodeId());
        assertEquals(456456, result.get(0).getSrcNode().getInterfaceId());
        assertEquals("x-hostname-x", result.get(0).getSrcNode().getForeignId());
        assertEquals(Locality.PUBLIC, result.get(0).getSrcLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getDstLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getFlowLocality());

        verifySameExcluding(
                testDocumentLog.getMessage(0),
                result.get(0),
                "src_node",
                "src_locality",
                "dst_locality",
                "flow_locality");
    }

    @Test
    void testEnrichNodeLookupOtherException() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(
                mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        StatusRuntimeException testException = new StatusRuntimeException(Status.INVALID_ARGUMENT);

        Mockito.when(mockInventoryClient.getIpInterfaceFromQuery("x-tenant-id-x", "1.1.1.1", "x-location-x"))
                .thenThrow(testException);

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLog);

        //
        // Verify the Results
        //
        assertFalse(result.get(0).hasSrcNode());
    }

    @Test
    void testEnrichClassifiable() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockClassificationRequest.isClassifiable()).thenReturn(true);
        Mockito.when(mockClassificationEngine.classify(mockClassificationRequest))
                .thenReturn("x-application-x");

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLog);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals("x-tenant-id-x", testDocumentLog.getTenantId());
        assertEquals("x-application-x", result.get(0).getApplication());
    }

    @Test
    void testEnrichPrivateLocality() {
        //
        // Setup Test Data and Interactions
        //
        var testDocumentLogPrivateLocality = TenantLocationSpecificFlowDocumentLog.newBuilder(testDocumentLog);
        testDocumentLogPrivateLocality
                .getMessageBuilder(0)
                .setSrcAddress("127.0.0.1")
                .setDstAddress("127.0.0.1")
                .build();

        //
        // Execute
        //
        List<FlowDocument> result = target.enrich(testDocumentLogPrivateLocality.build());

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals(Locality.PRIVATE, result.get(0).getFlowLocality());
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void verifySameExcluding(FlowDocument doc1, FlowDocument doc2, String... excludeField) {
        List<Descriptors.FieldDescriptor> fieldDescriptors =
                FlowDocument.getDescriptor().getFields();
        Set<String> excludedSet = new TreeSet<>(Arrays.asList(excludeField));

        for (Descriptors.FieldDescriptor oneFieldDescriptor : fieldDescriptors) {
            String fieldName = oneFieldDescriptor.getName();
            if (!excludedSet.contains(fieldName)) {
                Object value1 = doc1.getField(oneFieldDescriptor);
                Object value2 = doc2.getField(oneFieldDescriptor);

                assertEquals(value1, value2, "field " + fieldName + " must match");
            }
        }
    }
}
