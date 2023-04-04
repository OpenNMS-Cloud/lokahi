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

import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.DefaultNodeCreateDTO;
import org.opennms.horizon.inventory.dto.DefaultNodeDTO;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.grpc.node.DefaultNodeGrpcService;
import org.opennms.horizon.inventory.grpc.node.NodeGrpcService;
import org.opennms.horizon.inventory.mapper.node.DefaultNodeMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.node.DefaultNode;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.node.DefaultNodeService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.taskset.contract.ScanType;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

class DefaultNodeGrpcServiceTest {
    private DefaultNodeService mockDefaultNodeService;
    private IpInterfaceService mockIpInterfaceService;
    private DefaultNodeMapper mockDefaultNodeMapper;
    private TenantLookup mockTenantLookup;
    private ScannerTaskSetService mockScannerTaskSetService;
    private StreamObserver<DefaultNodeDTO> mockNodeDTOStreamObserver;
    private ExecutorService mockExecutorService;
    private Logger mockLogger;
    private DefaultNodeGrpcService target;
    private DefaultNode testNode;
    private DefaultNodeDTO testNodeDTO1;
    private DefaultNodeCreateDTO testNodeCreateDTO;
    private MonitoringLocation testMonitoringLocation;
    private Optional<String> testTenantIdOptional;

    @BeforeEach
    void setUp() {
        testNodeCreateDTO =
            DefaultNodeCreateDTO.newBuilder()
                .setLocation("x-location-x")
                .setManagementIp("12.0.0.1")
                .build();

        testMonitoringLocation = new MonitoringLocation();
        testMonitoringLocation.setLocation("x-monitoring-location-x");

        testNode = new DefaultNode();
        testNode.setNodeLabel("x-node-label-x");
        testNode.setTenantId("x-tenant-id-x");
        testNode.setMonitoringLocation(testMonitoringLocation);

        testNodeDTO1 = DefaultNodeDTO.newBuilder().setId(101010L).build();

        testTenantIdOptional = Optional.of("x-tenant-id-x");

        mockDefaultNodeService = Mockito.mock(DefaultNodeService.class);
        mockIpInterfaceService = Mockito.mock(IpInterfaceService.class);
        mockDefaultNodeMapper = Mockito.mock(DefaultNodeMapper.class);
        mockTenantLookup = Mockito.mock(TenantLookup.class);
        mockScannerTaskSetService = Mockito.mock(ScannerTaskSetService.class);
        mockNodeDTOStreamObserver = Mockito.mock(StreamObserver.class);
        mockExecutorService = Mockito.mock(ExecutorService.class);
        mockLogger = Mockito.mock(Logger.class);

        target =
            new DefaultNodeGrpcService(
                mockDefaultNodeService,
                mockIpInterfaceService,
                mockDefaultNodeMapper,
                mockTenantLookup,
                mockScannerTaskSetService);

        //
        // Common test interactions
        //
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class))).thenReturn(testTenantIdOptional);
    }

    /**
     * Verify the creation of a new node, and successful send of task updates.
     */
    @Test
    void testCreateNodeNewValidManagementIpSuccessfulSendTasks() {
        Runnable runnable = commonTestCreateNode();

        // Verify the lambda execution
        testSendTaskSetsToMinionLambda(runnable, testNode, testNodeDTO1);
    }


    /**
     * Verify the creation of a new node with no management IP address
     */
    @Test
    void testCreateNodeNoManagementIp() {
        //
        // Setup test data and interactions
        //
        testNodeCreateDTO =
            DefaultNodeCreateDTO.newBuilder()
                .setLocation("x-location-x")
                .build();

        //
        // Execute and Validate
        //
        commonTestCreateNode();
    }

    /**
     * Verify the creation of a new node, and successful send of task updates.
     */
    @Test
    void testCreateNodeInvalidManagementIp() {
        //
        // Setup test data and interactions
        //
        testNodeCreateDTO =
            DefaultNodeCreateDTO.newBuilder()
                .setManagementIp("INVALID-IP-ADDRESS")
                .build();

        //
        // Execute
        //
        target.createNode(testNodeCreateDTO, mockNodeDTOStreamObserver);

        //
        // Validate
        //
        var matcher = new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesInvalidArgument, "Bad management_ip: INVALID-IP-ADDRESS");
        Mockito.verify(mockNodeDTOStreamObserver).onError(Mockito.argThat(matcher));
    }

    /**
     * Verify the creation of a new node, and successful send of task updates.
     */
    @Test
    void testCreateNodeManagementInterfaceNotFound() {
        //
        // Setup test data and interactions
        //
        testNodeCreateDTO =
            DefaultNodeCreateDTO.newBuilder()
                .setManagementIp("127.0.0.1")
                .setLocation("x-location-x")
                .build();

        IpInterfaceDTO testInterfaceDTO =
            IpInterfaceDTO.newBuilder()
                .build();

        Mockito.when(mockIpInterfaceService.findByIpAddressAndLocationAndTenantId("127.0.0.1", "x-location-x", "x-tenant-id-x"))
            .thenReturn(Optional.of(testInterfaceDTO));

        //
        // Execute
        //
        target.createNode(testNodeCreateDTO, mockNodeDTOStreamObserver);

        //
        // Validate
        //
        var matcher =
            new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesAlreadyExistsValue,
                NodeGrpcService.IP_ADDRESS_ALREADY_EXISTS_FOR_LOCATION_MSG);

        Mockito.verify(mockNodeDTOStreamObserver).onError(Mockito.argThat(matcher));
    }

//========================================
// Internals
//----------------------------------------

    private Runnable commonTestCreateNode() {
        //
        // Setup test data and interactions
        //
        Mockito.when(mockDefaultNodeService.createNode(testNodeCreateDTO, ScanType.NODE_SCAN, "x-tenant-id-x")).thenReturn(testNode);
        Mockito.when(mockDefaultNodeMapper.modelToDto(testNode)).thenReturn(testNodeDTO1);

        //
        // Execute
        //
        target.setExecutorService(mockExecutorService);
        target.createNode(testNodeCreateDTO, mockNodeDTOStreamObserver);

        //
        // Validate
        //
        InOrder inOrder = Mockito.inOrder(mockNodeDTOStreamObserver);
        inOrder.verify(mockNodeDTOStreamObserver).onNext(testNodeDTO1);
        inOrder.verify(mockNodeDTOStreamObserver).onCompleted();

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockExecutorService).execute(argumentCaptor.capture());

        return argumentCaptor.getValue();
    }

    private void testSendTaskSetsToMinionLambda(Runnable runnable, DefaultNode testNode, DefaultNodeDTO testNodeDTO) {
        //
        // Execute
        //
        runnable.run();

        //
        // Validate
        //
        Mockito.verify(mockScannerTaskSetService).sendNodeScannerTask(List.of(testNodeDTO), "x-monitoring-location-x", "x-tenant-id-x");
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

    private boolean statusExceptionMatchesAlreadyExistsValue(Status status, Object expectedMessage) {
        if (status.getCode().value() == Code.ALREADY_EXISTS_VALUE) {
            if (status.getDescription() != null) {
                if (status.getDescription().equals(expectedMessage)) {
                    return true;
                }
            }
        }

        return false;
    }
//
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
}
