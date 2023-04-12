/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
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
package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.DefaultNodeDTO;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.MonitoredStateQuery;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.grpc.node.NodeGrpcService;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.node.NodeService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

class NodeGrpcServiceTest {
    private NodeService mockNodeService;
    private IpInterfaceService mockIpInterfaceService;
    private TenantLookup mockTenantLookup;
    private ScannerTaskSetService mockScannerTaskSetService;
    private StreamObserver<NodeDTO> mockNodeDTOStreamObserver;
    private StreamObserver<NodeList> mockNodeListStreamObserver;
    private StreamObserver<Int64Value> mockInt64ValueStreamObserver;
    private StreamObserver<BoolValue> mockBoolValueStreamObserver;

    private NodeGrpcService target;

    private NodeDTO testNodeDTO1;
    private MonitoringLocation testMonitoringLocation;
    private Optional<String> testTenantIdOptional;
    private List<NodeDTO> testNodeDTOList;

    @BeforeEach
    void setUp() {

        testMonitoringLocation = new MonitoringLocation();
        testMonitoringLocation.setLocation("x-monitoring-location-x");

        testNodeDTO1 = NodeDTO.newBuilder().setDefault(DefaultNodeDTO.newBuilder().setId(101010L).build()).build();

        testTenantIdOptional = Optional.of("x-tenant-id-x");

        testNodeDTOList = List.of(testNodeDTO1);


        mockNodeService = Mockito.mock(NodeService.class);
        mockIpInterfaceService = Mockito.mock(IpInterfaceService.class);
        mockTenantLookup = Mockito.mock(TenantLookup.class);
        mockScannerTaskSetService = Mockito.mock(ScannerTaskSetService.class);
        mockNodeDTOStreamObserver = Mockito.mock(StreamObserver.class);
        mockNodeListStreamObserver = Mockito.mock(StreamObserver.class);
        mockInt64ValueStreamObserver = Mockito.mock(StreamObserver.class);
        mockBoolValueStreamObserver = Mockito.mock(StreamObserver.class);

        target =
            new NodeGrpcService(
                mockNodeService,
                mockIpInterfaceService,
                mockTenantLookup,
                mockScannerTaskSetService);

        //
        // Common test interactions
        //
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class))).thenReturn(testTenantIdOptional);
    }

    @Test
    void testListNodes() {
        //
        // Setup test data and interactions
        //
        Empty testRequest = Empty.getDefaultInstance();

        Mockito.when(mockNodeService.findByTenantId("x-tenant-id-x")).thenReturn(testNodeDTOList);

        //
        // Execute
        //
        target.listNodes(testRequest, mockNodeListStreamObserver);

        //
        // Validate
        //
        InOrder inOrder = Mockito.inOrder(mockNodeListStreamObserver);
        inOrder.verify(mockNodeListStreamObserver).onNext(Mockito.argThat(argument -> {
            if (argument != null) {
                if (argument.getNodesCount() == testNodeDTOList.size()) {
                    return (argument.getNodesList().equals(testNodeDTOList));
                }
            }
            return false;
        }));
        inOrder.verify(mockNodeListStreamObserver).onCompleted();
    }

    @Test
    void testGetNodeByIdSuccessfulLookup() {
        //
        // Setup test data and interactions
        //
        Int64Value nodeIdRequest = Int64Value.newBuilder().setValue(131313L).build();
        Optional<NodeDTO> testNodeDTOOptional = Optional.of(testNodeDTO1);

        Mockito.when(mockNodeService.getByIdAndTenantId(131313L, "x-tenant-id-x")).thenReturn(testNodeDTOOptional);

        //
        // Execute
        //
        target.getNodeById(nodeIdRequest, mockNodeDTOStreamObserver);

        //
        // Validate
        //
        InOrder inOrder = Mockito.inOrder(mockNodeDTOStreamObserver);
        inOrder.verify(mockNodeDTOStreamObserver).onNext(testNodeDTO1);
        inOrder.verify(mockNodeDTOStreamObserver).onCompleted();
    }

    @Test
    void testGetNodeByIdFailedLookup() {
        //
        // Setup test data and interactions
        //
        Int64Value nodeIdRequest = Int64Value.newBuilder().setValue(242424L).build();
        Optional<NodeDTO> testNodeDTOOptional = Optional.empty();

        Mockito.when(mockNodeService.getByIdAndTenantId(131313L, "x-tenant-id-x")).thenReturn(testNodeDTOOptional);

        //
        // Execute
        //
        target.getNodeById(nodeIdRequest, mockNodeDTOStreamObserver);

        //
        // Validate
        //


        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesExpectedId, 242424L);
        Mockito.verify(mockNodeDTOStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetNodeIdFromQuery() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request =
            NodeIdQuery.newBuilder()
                .setLocation("x-location-x")
                .setIpAddress("127.0.0.1")
                .build();

        IpInterfaceDTO testIpInterfaceDTO =
            IpInterfaceDTO.newBuilder()
                .setHostname("x-hostname-x")
                .setNodeId(363636L)
                .build();
        Optional<IpInterfaceDTO> testOptionalIpInterfaceDTO = Optional.of(testIpInterfaceDTO);

        Mockito.when(
            mockIpInterfaceService.findByIpAddressAndLocationAndTenantId("127.0.0.1", "x-location-x", "x-tenant-id-x")
        ).thenReturn(testOptionalIpInterfaceDTO);

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        Int64ValueMatcher matcher = new Int64ValueMatcher(363636L);

        InOrder inOrder = Mockito.inOrder(mockInt64ValueStreamObserver);
        inOrder.verify(mockInt64ValueStreamObserver).onNext(Mockito.argThat(matcher));
        inOrder.verify(mockInt64ValueStreamObserver).onCompleted();
    }

    @Test
    void testListNodesByMonitoredState() {
        //
        // Setup test data and interactions
        //

        MonitoredStateQuery request = MonitoredStateQuery.newBuilder()
            .setMonitoredState(MonitoredState.DETECTED).build();

        Mockito.when(
            mockNodeService.findByMonitoredState("x-tenant-id-x", MonitoredState.DETECTED)
        ).thenReturn(testNodeDTOList);

        //
        // Execute
        //
        target.listNodesByMonitoredState(request, mockNodeListStreamObserver);

        //
        // Validate
        //
        InOrder inOrder = Mockito.inOrder(mockNodeListStreamObserver);
        inOrder.verify(mockNodeListStreamObserver).onNext(Mockito.argThat(argument -> {
            if (argument != null) {
                if (argument.getNodesCount() == testNodeDTOList.size()) {
                    return (argument.getNodesList().equals(testNodeDTOList));
                }
            }
            return false;
        }));
        inOrder.verify(mockNodeListStreamObserver).onCompleted();
    }

    @Test
    void testGetNodeIdFromQueryMissingTenantId() {
        //
        // Setup test data and interactions
        //

        // Reset the tenant lookup - don't use the common, default interaction that was already configured
        Mockito.reset(mockTenantLookup);
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class))).thenReturn(Optional.empty());

        NodeIdQuery request =
            NodeIdQuery.newBuilder()
                .setLocation("x-location-x")
                .setIpAddress("127.0.0.1")
                .build();

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher =
            new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesInvalidArgument, NodeGrpcService.EMPTY_TENANT_ID_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetNodeIdFromQueryMissingLocation() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request =
            NodeIdQuery.newBuilder()
                .setIpAddress("127.0.0.1")
                .build();

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher =
            new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesInvalidArgument, NodeGrpcService.INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void cetNodeIdFromQueryMissingIpAddress() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request =
            NodeIdQuery.newBuilder()
                .setLocation("x-location-x")
                .build();

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher =
            new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesInvalidArgument, NodeGrpcService.INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetNodeIdFromQueryNoMatchOnIpInterface() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request =
            NodeIdQuery.newBuilder()
                .setLocation("x-location-x")
                .setIpAddress("127.0.0.1")
                .build();

        Mockito.when(
            mockIpInterfaceService.findByIpAddressAndLocationAndTenantId("127.0.0.1", "x-location-x", "x-tenant-id-x")
        ).thenReturn(Optional.empty());

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher =
            new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesNotFound, NodeGrpcService.DIDNT_MATCH_NODE_ID_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteNodeSuccess() {
        //
        // Setup test data and interactions
        //
        Int64Value request =
            Int64Value.newBuilder()
                .setValue(111222L)
                .build();

        Optional<NodeDTO> testNodeDTOOptional = Optional.of(testNodeDTO1);
        Mockito.when(mockNodeService.getByIdAndTenantId(111222L, "x-tenant-id-x")).thenReturn(testNodeDTOOptional);


        //
        // Execute
        //
        target.deleteNode(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        BoolValueMatcher matcher = new BoolValueMatcher(true);
        InOrder inOrder = Mockito.inOrder(mockBoolValueStreamObserver);
        inOrder.verify(mockBoolValueStreamObserver).onNext(Mockito.argThat(matcher));
        inOrder.verify(mockBoolValueStreamObserver).onCompleted();
    }

    @Test
    void testDeleteNodeException() {
        //
        // Setup test data and interactions
        //
        Int64Value request =
            Int64Value.newBuilder()
                .setValue(101010L)
                .build();

        RuntimeException testException = new RuntimeException("x-test-exception-x");

        Optional<NodeDTO> testNodeDTOOptional = Optional.of(testNodeDTO1);
        Mockito.when(mockNodeService.getByIdAndTenantId(101010L, "x-tenant-id-x")).thenReturn(testNodeDTOOptional);
        Mockito.doThrow(testException).when(mockNodeService).deleteNode(101010L);


        //
        // Execute
        //
        target.deleteNode(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesDeleteError, 101010L);
        InOrder inOrder = Mockito.inOrder(mockBoolValueStreamObserver);
        inOrder.verify(mockBoolValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteNodeNotFound() {
        //
        // Setup test data and interactions
        //
        Int64Value request =
            Int64Value.newBuilder()
                .setValue(101010L)
                .build();

        Mockito.when(mockNodeService.getByIdAndTenantId(101010L, "x-tenant-id-x")).thenReturn(Optional.empty());

        //
        // Execute
        //
        target.deleteNode(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesExpectedId, 101010L);
        InOrder inOrder = Mockito.inOrder(mockBoolValueStreamObserver);
        inOrder.verify(mockBoolValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    //========================================
// Internals
//----------------------------------------
    private boolean statusExceptionMatchesExpectedId(Status status, Object expectedIdObj) {
        if (status.getCode().value() == Code.NOT_FOUND_VALUE) {
            if (status.getDescription() != null) {
                if (status.getDescription().equals("Node with id: " + expectedIdObj + " doesn't exist.")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesInvalidArgument(Status status, Object expectedMessage) {
        if (status.getCode().value() == Code.INVALID_ARGUMENT_VALUE) {
            if (status.getDescription() != null) {
                if (status.getDescription().equals(expectedMessage)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesNotFound(Status status, Object expectedMessage) {
        if (status.getCode().value() == Code.NOT_FOUND_VALUE) {
            if (status.getDescription() != null) {
                if (status.getDescription().equals(expectedMessage)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesDeleteError(Status status, Object expectedId) {
        if (status.getCode().value() == Code.INTERNAL_VALUE) {
            if (status.getDescription() != null) {
                if (status.getDescription().equals("Error while deleting node with ID " + expectedId)) {
                    return true;
                }
            }
        }

        return false;
    }

//========================================
// Custom Argument Matchers
//----------------------------------------

    private static class StatusRuntimeExceptionMatcher implements ArgumentMatcher<StatusRuntimeException> {

        private final BiFunction<Status, Object, Boolean> statusMatcher;
        private final Object data;

        public StatusRuntimeExceptionMatcher(BiFunction<Status, Object, Boolean> statusMatcher, Object data) {
            this.statusMatcher = statusMatcher;
            this.data = data;
        }

        @Override
        public boolean matches(StatusRuntimeException argument) {
            if (argument.getStatus() != null) {
                Status status = argument.getStatus();

                return this.statusMatcher.apply(status, data);
            }
            return false;
        }
    }

    private static class Int64ValueMatcher implements ArgumentMatcher<Int64Value> {
        private final long expectedValue;

        public Int64ValueMatcher(long expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean matches(Int64Value argument) {
            return argument.getValue() == expectedValue;
        }
    }

    private static class BoolValueMatcher implements ArgumentMatcher<BoolValue> {
        private final boolean expectedValue;

        public BoolValueMatcher(boolean expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean matches(BoolValue argument) {
            return (argument.getValue() == expectedValue);
        }
    }
}
