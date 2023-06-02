/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing;

import com.google.protobuf.Descriptors;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.document.Locality;
import org.opennms.horizon.flows.document.NetflowVersion;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class DocumentEnricherTest {

    private InventoryClient mockInventoryClient;
    private ClassificationEngine mockClassificationEngine;
    private FlowDocumentClassificationRequestMapper mockFlowDocumentClassificationRequestMapper;
    private ClassificationRequest mockClassificationRequest;

    private TenantLocationSpecificFlowDocument testDocument;

    private DocumentEnricherImpl target;

    @BeforeEach
    public void setUp() {
        mockInventoryClient = Mockito.mock(InventoryClient.class);
        mockClassificationEngine = Mockito.mock(ClassificationEngine.class);
        mockFlowDocumentClassificationRequestMapper = Mockito.mock(FlowDocumentClassificationRequestMapper.class);
        mockClassificationRequest = Mockito.mock(ClassificationRequest.class);

        testDocument =
            TenantLocationSpecificFlowDocument.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocation("x-location-x")
                .setSrcAddress("1.1.1.1")
                .setSrcPort(UInt32Value.of(510))
                .setDstAddress("2.2.2.2")
                .setDstPort(UInt32Value.of(80))
                .setProtocol(UInt32Value.of(6)) // TCP
                .setNetflowVersion(NetflowVersion.V5)
                .setExporterAddress("127.0.0.1")
                .build()
                ;

        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 0);

        Mockito.when(mockFlowDocumentClassificationRequestMapper.createClassificationRequest(Mockito.any(TenantLocationSpecificFlowDocument.class)))
            .thenReturn(mockClassificationRequest);
    }

    @Test
    void testEnrich() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocument);

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals("x-tenant-id-x", result.get(0).getTenantId());
    }

    @Test
    void testPositiveClockSkewThreshold() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);

        var now = Instant.now();
        int timeOffset = 100;

        TenantLocationSpecificFlowDocument testDocumentWithTimestamps =
            TenantLocationSpecificFlowDocument.newBuilder(testDocument)
                .setReceivedAt(now.plus(timeOffset, ChronoUnit.MILLIS).toEpochMilli())
                .setTimestamp(now.toEpochMilli())
                .setFirstSwitched(UInt64Value.of(now.minus(20_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setDeltaSwitched(UInt64Value.of(now.minus(10_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setLastSwitched(UInt64Value.of(now.minus(5_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .build()
                ;


        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocumentWithTimestamps);

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals("x-tenant-id-x", result.get(0).getTenantId());
        assertEquals(100, result.get(0).getClockCorrection());
        assertEquals(now.plus(Duration.ofMillis(100)).toEpochMilli(), result.get(0).getTimestamp()); // plus because skew is -100
        assertEquals(now.minus(19_900L, ChronoUnit.MILLIS).toEpochMilli(), result.get(0).getFirstSwitched().getValue());
        assertEquals(now.minus(9_900L, ChronoUnit.MILLIS).toEpochMilli(), result.get(0).getDeltaSwitched().getValue());
        assertEquals(now.minus(4_900L, ChronoUnit.MILLIS).toEpochMilli(), result.get(0).getLastSwitched().getValue());
        assertEquals(Locality.PUBLIC, result.get(0).getSrcLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getDstLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getFlowLocality());

        verifySameExcluding(
            testDocumentWithTimestamps,
            result.get(0),
            "first_switched", "delta_switched", "last_switched", "timestamp", "clock_correction", "src_locality", "dst_locality", "flow_locality"
        );
    }

    @Test
    void testPositiveClockSkewThresholdMissedBy1() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);

        var now = Instant.now();
        int timeOffset = 99;

        TenantLocationSpecificFlowDocument testDocumentWithTimestamps =
            TenantLocationSpecificFlowDocument.newBuilder(testDocument)
                .setReceivedAt(now.plus(timeOffset, ChronoUnit.MILLIS).toEpochMilli())
                .setTimestamp(now.toEpochMilli())
                .setFirstSwitched(UInt64Value.of(now.minus(20_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setDeltaSwitched(UInt64Value.of(now.minus(10_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .setLastSwitched(UInt64Value.of(now.minus(5_000L, ChronoUnit.MILLIS).toEpochMilli()))
                .build()
                ;


        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocumentWithTimestamps);

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals("x-tenant-id-x", result.get(0).getTenantId());
        assertEquals(0, result.get(0).getClockCorrection());
        assertEquals(Locality.PUBLIC, result.get(0).getSrcLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getDstLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getFlowLocality());

        verifySameExcluding(
            testDocumentWithTimestamps,
            result.get(0),
            "src_locality", "dst_locality", "flow_locality"
        );
    }

    @Test
    void testEnrichNone() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        List<TenantLocationSpecificFlowDocument> testList = Collections.emptyList();

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

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
        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocument);
        StatusRuntimeException testException = new StatusRuntimeException(Status.NOT_FOUND);

        Mockito.when(mockInventoryClient.getIpInterfaceFromQuery("x-tenant-id-x", "1.1.1.1", "x-location-x"))
            .thenThrow(testException);

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertFalse(result.get(0).hasSrcNode());
        assertEquals(Locality.PUBLIC, result.get(0).getSrcLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getDstLocality());
        assertEquals(Locality.PUBLIC, result.get(0).getFlowLocality());

        verifySameExcluding(
            testDocument,
            result.get(0),
            "src_locality", "dst_locality", "flow_locality"
        );
    }

    @Test
    void testEnrichNodeLookupFound() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocument);
        IpInterfaceDTO testIpInterfaceDTO =
            IpInterfaceDTO.newBuilder()
                .setNodeId(123123)
                .setId(456456)
                .setHostname("x-hostname-x")
                .build();

        Mockito.when(mockInventoryClient.getIpInterfaceFromQuery("x-tenant-id-x", "1.1.1.1", "x-location-x"))
            .thenReturn(testIpInterfaceDTO);

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

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
            testDocument,
            result.get(0),
            "src_node", "src_locality", "dst_locality", "flow_locality"
        );
    }

    @Test
    void testEnrichNodeLookupOtherException() {
        //
        // Setup Test Data and Interactions
        //
        target = new DocumentEnricherImpl(mockInventoryClient, mockClassificationEngine, mockFlowDocumentClassificationRequestMapper, 100);
        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocument);
        StatusRuntimeException testException = new StatusRuntimeException(Status.INVALID_ARGUMENT);

        Mockito.when(mockInventoryClient.getIpInterfaceFromQuery("x-tenant-id-x", "1.1.1.1", "x-location-x"))
            .thenThrow(testException);

        //
        // Execute
        //
        Exception actual = null;
        try {
            List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);
            fail("Missing expected exception");
        } catch (Exception caught) {
            actual = caught;
        }

        //
        // Verify the Results
        //
        assertSame(testException, actual);
    }

    @Test
    void testEnrichClassifiable() {
        //
        // Setup Test Data and Interactions
        //
        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocument);
        Mockito.when(mockClassificationRequest.isClassifiable()).thenReturn(true);
        Mockito.when(mockClassificationEngine.classify(mockClassificationRequest)).thenReturn("x-application-x");

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals("x-tenant-id-x", result.get(0).getTenantId());
        assertEquals("x-application-x", result.get(0).getApplication());
    }

    @Test
    void testEnrichPrivateLocality() {
        //
        // Setup Test Data and Interactions
        //
        TenantLocationSpecificFlowDocument testDocumentPrivateLocality =
            TenantLocationSpecificFlowDocument.newBuilder(testDocument)
                .setSrcAddress("127.0.0.1")
                .setDstAddress("127.0.0.1")
                .build()
                ;


        List<TenantLocationSpecificFlowDocument> testList = List.of(testDocumentPrivateLocality);

        //
        // Execute
        //
        List<TenantLocationSpecificFlowDocument> result = target.enrich(testList);

        //
        // Verify the Results
        //
        assertEquals(1, result.size());
        assertEquals(Locality.PRIVATE, result.get(0).getFlowLocality());
    }


//========================================
// Internals
//----------------------------------------

    private void verifySameExcluding(TenantLocationSpecificFlowDocument doc1, TenantLocationSpecificFlowDocument doc2, String... excludeField) {
        List<Descriptors.FieldDescriptor> fieldDescriptors = TenantLocationSpecificFlowDocument.getDescriptor().getFields();
        Set<String> excludedSet = new TreeSet<>(Arrays.asList(excludeField));

        for (Descriptors.FieldDescriptor oneFieldDescriptor : fieldDescriptors) {
            String fieldName = oneFieldDescriptor.getName();
            if (! excludedSet.contains(fieldName)) {
                Object value1 = doc1.getField(oneFieldDescriptor);
                Object value2 = doc2.getField(oneFieldDescriptor);

                assertEquals(value1, value2, "field " + fieldName + " must match");
            }
        }
    }

}
