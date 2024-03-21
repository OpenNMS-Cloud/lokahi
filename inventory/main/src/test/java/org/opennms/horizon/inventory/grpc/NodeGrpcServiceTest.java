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
package org.opennms.horizon.inventory.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.MonitoredStateQuery;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdList;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.dto.NodeUpdateDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.SnmpInterfaceService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.taskset.contract.ScanType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NodeGrpcServiceTest {
    private static final String TEST_LOCATION_NAME = "x-location-x";
    private static final long TEST_LOCATION_ID = 5L;
    private static final String TEST_TENANT_ID = "x-tenant-id-x";
    private NodeService mockNodeService;
    private IpInterfaceService mockIpInterfaceService;
    private NodeMapper mockNodeMapper;
    private TenantLookup mockTenantLookup;
    private ScannerTaskSetService mockScannerTaskSetService;
    private StreamObserver<NodeDTO> mockNodeDTOStreamObserver;
    private StreamObserver<NodeList> mockNodeListStreamObserver;
    private StreamObserver<Int64Value> mockInt64ValueStreamObserver;
    private StreamObserver<BoolValue> mockBoolValueStreamObserver;
    private ExecutorService mockExecutorService;
    private MonitoringLocationService mockMonitoringLocationService;
    private NodeGrpcService target;

    private SnmpInterfaceService mockSnmpInterfaceService;
    private Node testNode;
    private NodeDTO testNodeDTO1;
    private NodeDTO testNodeDTO2A;
    private NodeDTO testNodeDTO2B;
    private NodeCreateDTO testNodeCreateDTO;
    private MonitoringLocation testMonitoringLocation;
    private Optional<String> testTenantIdOptional;
    private List<NodeDTO> testNodeDTOList;

    private TagService mockTagService;

    @BeforeEach
    void setUp() {
        testNodeCreateDTO = NodeCreateDTO.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .setManagementIp("12.0.0.1")
                .build();

        testMonitoringLocation = new MonitoringLocation();
        testMonitoringLocation.setId(TEST_LOCATION_ID);
        testMonitoringLocation.setLocation(TEST_LOCATION_NAME);

        testNode = new Node();
        testNode.setNodeLabel("x-node-label-x");
        testNode.setTenantId(TEST_TENANT_ID);
        testNode.setMonitoringLocation(testMonitoringLocation);

        testNodeDTO1 = NodeDTO.newBuilder().setId(101010L).build();
        testNodeDTO2A = NodeDTO.newBuilder().setId(202020L).build();
        testNodeDTO2B = NodeDTO.newBuilder().setId(303030L).build();

        testTenantIdOptional = Optional.of(TEST_TENANT_ID);

        testNodeDTOList = List.of(testNodeDTO1);

        mockNodeService = Mockito.mock(NodeService.class);
        mockIpInterfaceService = Mockito.mock(IpInterfaceService.class);
        mockNodeMapper = Mockito.mock(NodeMapper.class);
        mockTenantLookup = Mockito.mock(TenantLookup.class);
        mockScannerTaskSetService = Mockito.mock(ScannerTaskSetService.class);
        mockNodeDTOStreamObserver = Mockito.mock(StreamObserver.class);
        mockNodeListStreamObserver = Mockito.mock(StreamObserver.class);
        mockInt64ValueStreamObserver = Mockito.mock(StreamObserver.class);
        mockBoolValueStreamObserver = Mockito.mock(StreamObserver.class);
        mockExecutorService = Mockito.mock(ExecutorService.class);
        mockMonitoringLocationService = Mockito.mock(MonitoringLocationService.class);
        mockTagService = Mockito.mock(TagService.class);

        Optional<MonitoringLocationDTO> locationDto = Optional.of(MonitoringLocationDTO.newBuilder()
                .setId(TEST_LOCATION_ID)
                .setLocation(TEST_LOCATION_NAME)
                .setTenantId(TEST_TENANT_ID)
                .build());
        when(mockMonitoringLocationService.findByLocationIdAndTenantId(TEST_LOCATION_ID, TEST_TENANT_ID))
                .thenReturn(locationDto);

        target = new NodeGrpcService(
                mockNodeService,
                mockIpInterfaceService,
                mockNodeMapper,
                mockTenantLookup,
                mockScannerTaskSetService,
                mockMonitoringLocationService,
                mockSnmpInterfaceService,
                mockTagService);

        //
        // Common test interactions
        //
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class)))
                .thenReturn(testTenantIdOptional);
    }

    /**
     * Verify the creation of a new node, and successful send of task updates.
     */
    @Test
    void testCreateNodeNewValidManagementIpSuccessfulSendTasks()
            throws EntityExistException, LocationNotFoundException {
        Runnable runnable = commonTestCreateNode();

        // Verify the lambda execution
        testSendTaskSetsToMinionLambda(runnable, testNode, testNodeDTO1);
    }

    /**
     * Verify the creation of a new node with no management IP address
     */
    @Test
    void testCreateNodeNoManagementIp() throws EntityExistException, LocationNotFoundException {
        //
        // Setup test data and interactions
        //
        testNodeCreateDTO = NodeCreateDTO.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
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
                NodeCreateDTO.newBuilder().setManagementIp("INVALID-IP-ADDRESS").build();

        //
        // Execute
        //
        target.createNode(testNodeCreateDTO, mockNodeDTOStreamObserver);

        //
        // Validate
        //
        var matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesInvalidArgument, "Bad management_ip: INVALID-IP-ADDRESS");
        Mockito.verify(mockNodeDTOStreamObserver).onError(Mockito.argThat(matcher));
    }

    /**
     * Verify the creation of a new node, and successful send of task updates.
     */
    @Test
    void testCreateNodeEntityExistException() throws EntityExistException, LocationNotFoundException {
        //
        // Setup test data and interactions
        //
        testNodeCreateDTO = NodeCreateDTO.newBuilder()
                .setManagementIp("127.0.0.1")
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .build();

        doThrow(new EntityExistException("IP exists"))
                .when(mockNodeService)
                .createNode(testNodeCreateDTO, ScanType.NODE_SCAN, TEST_TENANT_ID);
        //
        // Execute
        //
        target.createNode(testNodeCreateDTO, mockNodeDTOStreamObserver);

        //
        // Validate
        //
        var matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesAlreadyExistsValue,
                NodeGrpcService.IP_ADDRESS_ALREADY_EXISTS_FOR_LOCATION_MSG);

        Mockito.verify(mockNodeDTOStreamObserver).onError(Mockito.argThat(matcher));
        verify(mockNodeService).createNode(testNodeCreateDTO, ScanType.NODE_SCAN, TEST_TENANT_ID);
    }

    /**
     * Verify new node is not created if the location doesn't exist.
     */
    @Test
    void testCreateNodeLocationNotFoundException() throws EntityExistException, LocationNotFoundException {
        //
        // Setup test data and interactions
        //
        testNodeCreateDTO = NodeCreateDTO.newBuilder()
                .setManagementIp("127.0.0.1")
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .build();

        doThrow(new LocationNotFoundException("Location not found"))
                .when(mockNodeService)
                .createNode(testNodeCreateDTO, ScanType.NODE_SCAN, TEST_TENANT_ID);
        //
        // Execute
        //
        target.createNode(testNodeCreateDTO, mockNodeDTOStreamObserver);

        //
        // Validate
        //
        var matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesNotFoundValue,
                NodeGrpcService.INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG);

        Mockito.verify(mockNodeDTOStreamObserver).onError(Mockito.argThat(matcher));
        verify(mockNodeService).createNode(testNodeCreateDTO, ScanType.NODE_SCAN, TEST_TENANT_ID);
    }

    @Test
    void testListNodes() {
        //
        // Setup test data and interactions
        //
        Empty testRequest = Empty.getDefaultInstance();

        Mockito.when(mockNodeService.findByTenantId(TEST_TENANT_ID)).thenReturn(testNodeDTOList);

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

        Mockito.when(mockNodeService.getByIdAndTenantId(131313L, TEST_TENANT_ID))
                .thenReturn(testNodeDTOOptional);

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

        Mockito.when(mockNodeService.getByIdAndTenantId(131313L, TEST_TENANT_ID))
                .thenReturn(testNodeDTOOptional);

        //
        // Execute
        //
        target.getNodeById(nodeIdRequest, mockNodeDTOStreamObserver);

        //
        // Validate
        //

        StatusRuntimeExceptionMatcher matcher =
                new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesExpectedId, 242424L);
        Mockito.verify(mockNodeDTOStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetNodeIdFromQuery() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .setIpAddress("127.0.0.1")
                .build();

        IpInterfaceDTO testIpInterfaceDTO = IpInterfaceDTO.newBuilder()
                .setHostname("x-hostname-x")
                .setNodeId(363636L)
                .build();
        Optional<IpInterfaceDTO> testOptionalIpInterfaceDTO = Optional.of(testIpInterfaceDTO);

        Mockito.when(mockIpInterfaceService.findByIpAddressAndLocationIdAndTenantId(
                        "127.0.0.1", String.valueOf(TEST_LOCATION_ID), TEST_TENANT_ID))
                .thenReturn(testOptionalIpInterfaceDTO);

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
    void testGetNodeIdFromQueryInvalidLocation() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder().setLocationId("11").build();

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesNotFound, NodeGrpcService.INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testListNodesByMonitoredState() {
        //
        // Setup test data and interactions
        //

        MonitoredStateQuery request = MonitoredStateQuery.newBuilder()
                .setMonitoredState(MonitoredState.DETECTED)
                .build();

        Mockito.when(mockNodeService.findByMonitoredState(TEST_TENANT_ID, MonitoredState.DETECTED))
                .thenReturn(testNodeDTOList);

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
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class)))
                .thenReturn(Optional.empty());

        NodeIdQuery request = NodeIdQuery.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .setIpAddress("127.0.0.1")
                .build();

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesInvalidArgument, NodeGrpcService.EMPTY_TENANT_ID_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetNodeIdFromQueryMissingLocation() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .setIpAddress("127.0.0.1")
                .build();

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesNotFound, NodeGrpcService.DIDNT_MATCH_NODE_ID_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetNodeIdFromQueryMissingIpAddress() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .build();

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesInvalidArgument,
                NodeGrpcService.INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetNodeIdFromQueryNoMatchOnIpInterface() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .setIpAddress("127.0.0.1")
                .build();

        Mockito.when(mockIpInterfaceService.findByIpAddressAndLocationIdAndTenantId(
                        "127.0.0.1", String.valueOf(TEST_LOCATION_ID), TEST_TENANT_ID))
                .thenReturn(Optional.empty());

        //
        // Execute
        //
        target.getNodeIdFromQuery(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesNotFound, NodeGrpcService.DIDNT_MATCH_NODE_ID_MSG);
        Mockito.verify(mockInt64ValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testUpdateNodeSuccess() {
        //
        // Setup test data and interactions
        //
        NodeUpdateDTO request = NodeUpdateDTO.newBuilder()
                .setTenantId(TEST_TENANT_ID)
                .setId(testNodeDTO1.getId())
                .setNodeAlias("AAA")
                .build();

        Node node = new Node();
        node.setTenantId(request.getTenantId());
        node.setId(request.getId());
        node.setNodeAlias(request.getNodeAlias());

        var nodeDto = NodeDTO.newBuilder()
                .setTenantId(request.getTenantId())
                .setId(request.getId())
                .setNodeAlias(request.getNodeAlias())
                .build();

        when(mockNodeService.updateNode(request, TEST_TENANT_ID)).thenReturn(testNodeDTO1.getId());
        when(mockNodeMapper.modelToDTO(node)).thenReturn(nodeDto);

        //
        // Execute
        //
        target.updateNode(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        InOrder inOrder = Mockito.inOrder(mockInt64ValueStreamObserver);
        inOrder.verify(mockInt64ValueStreamObserver).onNext(Int64Value.of(testNodeDTO1.getId()));
        inOrder.verify(mockInt64ValueStreamObserver).onCompleted();
    }

    @Test
    void testUpdateNodeException() {
        //
        // Setup test data and interactions
        //
        NodeUpdateDTO request = NodeUpdateDTO.newBuilder()
                .setTenantId(TEST_TENANT_ID)
                .setId(testNodeDTO1.getId())
                .setNodeAlias("AAA")
                .build();

        var testException = new InventoryRuntimeException("x-test-exception-x");
        Mockito.doThrow(testException).when(mockNodeService).updateNode(request, TEST_TENANT_ID);

        //
        // Execute
        //
        target.updateNode(request, mockInt64ValueStreamObserver);

        //
        // Validate
        //
        InOrder inOrder = Mockito.inOrder(mockInt64ValueStreamObserver);
        inOrder.verify(mockInt64ValueStreamObserver).onError(any(StatusRuntimeException.class));
    }

    @Test
    void testDeleteNodeSuccess() {
        //
        // Setup test data and interactions
        //
        Int64Value request = Int64Value.newBuilder().setValue(111222L).build();

        Optional<NodeDTO> testNodeDTOOptional = Optional.of(testNodeDTO1);
        Mockito.when(mockNodeService.getByIdAndTenantId(111222L, TEST_TENANT_ID))
                .thenReturn(testNodeDTOOptional);

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
        Int64Value request = Int64Value.newBuilder().setValue(101010L).build();

        RuntimeException testException = new RuntimeException("x-test-exception-x");

        Optional<NodeDTO> testNodeDTOOptional = Optional.of(testNodeDTO1);
        Mockito.when(mockNodeService.getByIdAndTenantId(101010L, TEST_TENANT_ID))
                .thenReturn(testNodeDTOOptional);
        Mockito.doThrow(testException).when(mockNodeService).deleteNode(101010L);

        //
        // Execute
        //
        target.deleteNode(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher =
                new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesDeleteError, 101010L);
        InOrder inOrder = Mockito.inOrder(mockBoolValueStreamObserver);
        inOrder.verify(mockBoolValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteNodeNotFound() {
        //
        // Setup test data and interactions
        //
        Int64Value request = Int64Value.newBuilder().setValue(101010L).build();

        Mockito.when(mockNodeService.getByIdAndTenantId(101010L, TEST_TENANT_ID))
                .thenReturn(Optional.empty());

        //
        // Execute
        //
        target.deleteNode(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher =
                new StatusRuntimeExceptionMatcher(this::statusExceptionMatchesExpectedId, 101010L);
        InOrder inOrder = Mockito.inOrder(mockBoolValueStreamObserver);
        inOrder.verify(mockBoolValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testStartNodeScanByIdsSuccess() {
        //
        // Setup test data and interactions
        //
        NodeIdList request = NodeIdList.newBuilder()
                .addIds(101010L)
                .addIds(202020L)
                .addIds(303030L)
                .build();

        Map<Long, List<NodeDTO>> testNodeByLocationMap = Map.of(
                1001L, List.of(testNodeDTO1),
                1002L, List.of(testNodeDTO2A, testNodeDTO2B));

        Mockito.when(mockNodeService.listNodeByIds(request.getIdsList(), TEST_TENANT_ID))
                .thenReturn(testNodeByLocationMap);

        //
        // Execute
        //
        target.setExecutorService(mockExecutorService);
        target.startNodeScanByIds(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        BoolValueMatcher boolValueMatcher = new BoolValueMatcher(true);
        InOrder inOrder = Mockito.inOrder(mockBoolValueStreamObserver);
        inOrder.verify(mockBoolValueStreamObserver).onNext(Mockito.argThat(boolValueMatcher));
        inOrder.verify(mockBoolValueStreamObserver).onCompleted();

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockExecutorService).execute(argumentCaptor.capture());

        //
        // Execute 2 - call the runnable
        //
        Runnable sendScannerTasksToMinionRunnable = argumentCaptor.getValue();
        sendScannerTasksToMinionRunnable.run();

        //
        // Validate 2
        //
        Mockito.verify(mockScannerTaskSetService).sendNodeScannerTask(List.of(testNodeDTO1), 1001L, TEST_TENANT_ID);
        Mockito.verify(mockScannerTaskSetService)
                .sendNodeScannerTask(List.of(testNodeDTO2A, testNodeDTO2B), 1002L, TEST_TENANT_ID);

        // Make sure those 2 calls are all of them
        Mockito.verify(mockScannerTaskSetService, Mockito.times(2))
                .sendNodeScannerTask(Mockito.any(List.class), Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    void testStartNodeScanByNoNodesFound() {
        //
        // Setup test data and interactions
        //
        NodeIdList request = NodeIdList.newBuilder()
                .addIds(101010L)
                .addIds(202020L)
                .addIds(303030L)
                .build();

        Map<Long, List<NodeDTO>> testNodeByLocationMap = Collections.EMPTY_MAP;
        Mockito.when(mockNodeService.listNodeByIds(request.getIdsList(), TEST_TENANT_ID))
                .thenReturn(testNodeByLocationMap);

        //
        // Execute
        //
        target.setExecutorService(mockExecutorService);
        target.startNodeScanByIds(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesNotFound, "No nodes exist with ids " + request.getIdsList());
        Mockito.verify(mockBoolValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testStartNodeScanMissingTenantId() {
        //
        // Setup test data and interactions
        //
        NodeIdList request = NodeIdList.newBuilder()
                .addIds(101010L)
                .addIds(202020L)
                .addIds(303030L)
                .build();

        // Reset the tenant lookup - don't use the common, default interaction that was already configured
        Mockito.reset(mockTenantLookup);
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class)))
                .thenReturn(Optional.empty());

        //
        // Execute
        //
        target.startNodeScanByIds(request, mockBoolValueStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesInvalidArgument, NodeGrpcService.TENANT_ID_IS_MISSING_MSG);
        Mockito.verify(mockBoolValueStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetIpInterfaceFromQuery() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .setIpAddress("192.168.0.1")
                .build();
        IpInterfaceDTO ipInterfaceDTO = IpInterfaceDTO.newBuilder()
                .setHostname("x-hostname-x")
                .setIpAddress(request.getIpAddress())
                .setNodeId(363636L)
                .build();
        StreamObserver<IpInterfaceDTO> mockIpInterfaceDTOStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockIpInterfaceService.findByIpAddressAndLocationIdAndTenantId(
                        request.getIpAddress(), request.getLocationId(), TEST_TENANT_ID))
                .thenReturn(Optional.of(ipInterfaceDTO));

        //
        // Execute
        //
        target.getIpInterfaceFromQuery(request, mockIpInterfaceDTOStreamObserver);

        //
        // Validate
        //

        Mockito.verify(mockIpInterfaceDTOStreamObserver).onNext(ipInterfaceDTO);
    }

    @Test
    void testGetIpInterfaceFromQueryNotFound() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder().setLocationId("9999").build();
        StreamObserver<IpInterfaceDTO> mockIpInterfaceDTOStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.getIpInterfaceFromQuery(request, mockIpInterfaceDTOStreamObserver);

        //
        // Validate
        //

        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesNotFound, NodeGrpcService.INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG);
        Mockito.verify(mockIpInterfaceDTOStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetIpInterfaceFromMissingTenant() {
        //
        // Setup test data and interactions
        //
        NodeIdQuery request = NodeIdQuery.newBuilder().build();

        // Reset the tenant lookup - don't use the common, default interaction that was already configured
        Mockito.reset(mockTenantLookup);
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class)))
                .thenReturn(Optional.empty());
        StreamObserver<IpInterfaceDTO> mockIpInterfaceDTOStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.getIpInterfaceFromQuery(request, mockIpInterfaceDTOStreamObserver);

        //
        // Validate
        //
        StatusRuntimeExceptionMatcher matcher = new StatusRuntimeExceptionMatcher(
                this::statusExceptionMatchesInvalidArgument, NodeGrpcService.TENANT_ID_IS_MISSING_MSG);
        Mockito.verify(mockIpInterfaceDTOStreamObserver).onError(Mockito.argThat(matcher));
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private Runnable commonTestCreateNode() throws EntityExistException, LocationNotFoundException {
        //
        // Setup test data and interactions
        //
        Mockito.when(mockNodeService.createNode(testNodeCreateDTO, ScanType.NODE_SCAN, TEST_TENANT_ID))
                .thenReturn(testNode);
        Mockito.when(mockNodeMapper.modelToDTO(testNode)).thenReturn(testNodeDTO1);

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

    private void testSendTaskSetsToMinionLambda(Runnable runnable, Node testNode, NodeDTO testNodeDTO) {
        //
        // Execute
        //
        runnable.run();

        //
        // Validate
        //
        Mockito.verify(mockScannerTaskSetService)
                .sendNodeScannerTask(List.of(testNodeDTO), testNode.getMonitoringLocationId(), TEST_TENANT_ID);
    }

    private boolean statusExceptionMatchesExpectedId(Status status, Object expectedIdObj) {
        if (status.getCode().value() == Code.NOT_FOUND_VALUE) {
            if (status.getDescription() != null) {
                return status.getDescription().equals("Node with id: " + expectedIdObj + " doesn't exist.");
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesInvalidArgument(Status status, Object expectedMessage) {
        if (status.getCode().value() == Code.INVALID_ARGUMENT_VALUE) {
            if (status.getDescription() != null) {
                return status.getDescription().equals(expectedMessage);
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesNotFound(Status status, Object expectedMessage) {
        if (status.getCode().value() == Code.NOT_FOUND_VALUE) {
            if (status.getDescription() != null) {
                return status.getDescription().equals(expectedMessage);
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesDeleteError(Status status, Object expectedId) {
        if (status.getCode().value() == Code.INTERNAL_VALUE) {
            if (status.getDescription() != null) {
                return status.getDescription().equals("Error while deleting node with ID " + expectedId);
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesAlreadyExistsValue(Status status, Object expectedMessage) {
        if (status.getCode().value() == Code.ALREADY_EXISTS_VALUE) {
            if (status.getDescription() != null) {
                return status.getDescription().equals(expectedMessage);
            }
        }

        return false;
    }

    private boolean statusExceptionMatchesNotFoundValue(Status status, Object expectedMessage) {
        if (status.getCode().value() == Code.NOT_FOUND_VALUE) {
            if (status.getDescription() != null) {
                return status.getDescription().equals(expectedMessage);
            }
        }

        return false;
    }

    // ========================================
    // Custom Argument Matchers
    // ----------------------------------------

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
