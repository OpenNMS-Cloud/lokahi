/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.server.service.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemList;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class InventoryClientTest {
    @Rule
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static InventoryClient client;
    private static MockServerInterceptor mockInterceptor;
    private static MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase mockLocationService;
    private static NodeServiceGrpc.NodeServiceImplBase mockNodeService;
    private static MonitoringSystemServiceGrpc.MonitoringSystemServiceImplBase mockSystemService;
    private final String accessToken = "test-token";

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockInterceptor = new MockServerInterceptor();

        mockLocationService = mock(MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase.class, delegatesTo(
            new MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase() {
                @Override
                public void listLocations(Empty request, StreamObserver<MonitoringLocationList> responseObserver) {
                    responseObserver.onNext(MonitoringLocationList.newBuilder().build());
                    responseObserver.onCompleted();
                }

               @Override
                public void getLocationById(Int64Value request, StreamObserver<MonitoringLocationDTO> responseObserver) {
                    responseObserver.onNext(MonitoringLocationDTO.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void listLocationsByIds(IdList request, StreamObserver<MonitoringLocationList> responseObserver) {
                    responseObserver.onNext(MonitoringLocationList.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void createLocation(MonitoringLocationDTO request, StreamObserver<MonitoringLocationDTO> responseObserver) {
                    responseObserver.onNext(MonitoringLocationDTO.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void updateLocation(MonitoringLocationDTO request, StreamObserver<MonitoringLocationDTO> responseObserver) {
                    responseObserver.onNext(MonitoringLocationDTO.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void deleteLocation(Int64Value request, StreamObserver<BoolValue> responseObserver) {
                    responseObserver.onNext(BoolValue.of(true));
                    responseObserver.onCompleted();
                }
            }));
        mockNodeService = mock(NodeServiceGrpc.NodeServiceImplBase.class, delegatesTo(
            new NodeServiceGrpc.NodeServiceImplBase(){
                @Override
                public void createNode(NodeCreateDTO request, StreamObserver<NodeDTO> responseObserver) {
                    responseObserver.onNext(NodeDTO.newBuilder()
                        .setNodeLabel(request.getLabel()).build());
                    responseObserver.onCompleted();
                }

                @Override
                public void listNodes(Empty request, StreamObserver<NodeList> responseObserver) {
                    responseObserver.onNext(NodeList.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void getNodeById(Int64Value request, StreamObserver<NodeDTO> responseObserver) {
                    responseObserver.onNext(NodeDTO.newBuilder().build());
                    responseObserver.onCompleted();
                }
            }));

        mockSystemService = mock(MonitoringSystemServiceGrpc.MonitoringSystemServiceImplBase.class, delegatesTo(
            new MonitoringSystemServiceGrpc.MonitoringSystemServiceImplBase() {
                @Override
                public void listMonitoringSystem(Empty request, StreamObserver<MonitoringSystemList> responseObserver) {
                    responseObserver.onNext(MonitoringSystemList.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void getMonitoringSystemById(StringValue request, StreamObserver<MonitoringSystemDTO> responseObserver) {
                    responseObserver.onNext(MonitoringSystemDTO.newBuilder().build());
                    responseObserver.onCompleted();
                }
            }));

        grpcCleanUp.register(InProcessServerBuilder.forName("InventoryClientTest").intercept(mockInterceptor)
            .addService(mockLocationService)
            .addService(mockSystemService)
            .addService(mockNodeService).directExecutor().build().start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("InventoryClientTest").directExecutor().build());
        client = new InventoryClient(channel, 5000);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockLocationService);
        verifyNoMoreInteractions(mockSystemService);
        verifyNoMoreInteractions(mockNodeService);
        reset(mockNodeService, mockSystemService, mockLocationService);
        mockInterceptor.reset();
    }

    @Test
    void testListLocation() {
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<Empty> captor = ArgumentCaptor.forClass(Empty.class);
        List<MonitoringLocationDTO> result = client.listLocations(accessToken + methodName);
        assertThat(result).isEmpty();
        verify(mockLocationService).listLocations(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testListLocationsByIds() {
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        List<Long> ids = Arrays.asList(1L, 2L);
        List<DataLoaderFactory.Key> keys = ids.stream().map(id -> new DataLoaderFactory.Key(id, accessToken + methodName)).collect(Collectors.toList());
        ArgumentCaptor<IdList> captor = ArgumentCaptor.forClass(IdList.class);
        List<MonitoringLocationDTO> result = client.listLocationsByIds(keys);
        assertThat(result).isEmpty();
        verify(mockLocationService).listLocationsByIds(captor.capture(), any());
        List<Int64Value> argList = captor.getValue().getIdsList();
        assertThat(argList).hasSameSizeAs(keys);
        argList.forEach(v -> assertThat(ids).contains(v.getValue()));
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testGetLocationById() {
        long locationId = 100L;
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<Int64Value> captor = ArgumentCaptor.forClass(Int64Value.class);
        MonitoringLocationDTO result = client.getLocationById(locationId, accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockLocationService).getLocationById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(locationId);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testCreateNewNode() {
        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder().setLabel("test-node").build();
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<NodeCreateDTO> captor = ArgumentCaptor.forClass(NodeCreateDTO.class);
        NodeDTO result = client.createNewNode(createDTO, accessToken + methodName);
        assertThat(result).isNotNull();
        assertThat(result.getNodeLabel()).isEqualTo(createDTO.getLabel());
        verify(mockNodeService).createNode(captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo(createDTO);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testListNodes() {
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<Empty> captor = ArgumentCaptor.forClass(Empty.class);
        List<NodeDTO> result = client.listNodes(accessToken + methodName);
        assertThat(result).isEmpty();
        verify(mockNodeService).listNodes(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testGetNodeById() {
        long locationId = 100L;
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<Int64Value> captor = ArgumentCaptor.forClass(Int64Value.class);
        NodeDTO result = client.getNodeById(locationId, accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockNodeService).getNodeById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(locationId);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testListMonitoringSystem() {
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<Empty> captor = ArgumentCaptor.forClass(Empty.class);
        List<MonitoringSystemDTO> result = client.listMonitoringSystems(accessToken + methodName);
        assertThat(result).isEmpty();
        verify(mockSystemService).listMonitoringSystem(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testGetMonitoringSystemBySystemId() {
        String systemId = "test-system-id-123";
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<StringValue> captor = ArgumentCaptor.forClass(StringValue.class);
        MonitoringSystemDTO result = client.getSystemBySystemId(systemId, accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockSystemService).getMonitoringSystemById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(systemId);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testCreateLocation() {
        MonitoringLocationDTO createDTO = MonitoringLocationDTO.newBuilder().setLocation("test-location").build();
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<MonitoringLocationDTO> captor = ArgumentCaptor.forClass(MonitoringLocationDTO.class);
        MonitoringLocationDTO result = client.createLocation(createDTO, accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockLocationService).createLocation(captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo(createDTO);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testUpdateLocation() {
        MonitoringLocationDTO updateDTO = MonitoringLocationDTO.newBuilder().setLocation("test-location").build();
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<MonitoringLocationDTO> captor = ArgumentCaptor.forClass(MonitoringLocationDTO.class);
        MonitoringLocationDTO result = client.updateLocation(updateDTO, accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockLocationService).updateLocation(captor.capture(), any());
        assertThat(captor.getValue()).isEqualTo(updateDTO);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testDeleteLocation() {
        long locationId = 1L;
        String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<Int64Value> captor = ArgumentCaptor.forClass(Int64Value.class);
        client.deleteLocation(locationId, accessToken + methodName);
        verify(mockLocationService).deleteLocation(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(locationId);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    private static class MockServerInterceptor implements ServerInterceptor {
        private String authHeader;
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            authHeader = headers.get(GrpcConstants.AUTHORIZATION_METADATA_KEY);
            return next.startCall(call, headers);
        }

        public String getAuthHeader() {
            return authHeader;
        }
        public void reset() {
            authHeader = null;
        }
    }

}
