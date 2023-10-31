package org.opennms.horizon.events.grpc.service;

import com.google.protobuf.UInt64Value;
import com.google.rpc.Code;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.opennms.horizon.events.grpc.client.InventoryClient;
import org.opennms.horizon.events.grpc.config.GrpcTenantLookupImpl;
import org.opennms.horizon.events.grpc.config.TenantLookup;
import org.opennms.horizon.events.persistence.service.EventService;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventGrpcServiceTest extends AbstractGrpcUnitTest {
    private EventServiceGrpc.EventServiceBlockingStub stub;
    private EventService mockEventService;
    private EventGrpcService eventGrpcService;
    private InventoryClient mockInventoryClient;
    private ManagedChannel channel;
    protected TenantLookup tenantLookup = new GrpcTenantLookupImpl();

    public static final String TEST_TENANTID = "test-tenant";
    public static final long TEST_NODEID = 1L;

    @BeforeEach
    public void prepareTest() throws VerificationException, IOException {
        mockEventService = Mockito.mock(EventService.class);
        mockInventoryClient = Mockito.mock(InventoryClient.class);
        eventGrpcService = new EventGrpcService(mockEventService, mockInventoryClient, tenantLookup);

        startServer(eventGrpcService);
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = EventServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void afterTest() throws InterruptedException {
        Mockito.verifyNoMoreInteractions(mockInventoryClient);
        Mockito.verifyNoMoreInteractions(mockEventService);
        Mockito.verifyNoMoreInteractions(spyInterceptor);
        Mockito.reset(mockEventService, mockInventoryClient, spyInterceptor);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }


    @Test
    void testGetEventsByNodeId() throws VerificationException {
        NodeDTO node = NodeDTO.newBuilder().setId(TEST_NODEID).build();
        Event e1 = Event.newBuilder().setNodeId(TEST_NODEID).setTenantId(TEST_TENANTID).setUei("uei1").build();
        Event e2 = Event.newBuilder().setNodeId(TEST_NODEID).setTenantId(TEST_TENANTID).setUei("uei2").build();
        Mockito.when(mockInventoryClient.getNodeById(TEST_TENANTID, TEST_NODEID)).thenReturn(node);
        Mockito.when(mockEventService.findEventsByNodeId(TEST_TENANTID, TEST_NODEID)).thenReturn(List.of(e1, e2));

        EventLog result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .getEventsByNodeId(UInt64Value.of(TEST_NODEID));

        assertThat(result.getEventsList()).hasSize(2);
        Mockito.verify(mockInventoryClient, Mockito.times(1)).getNodeById(TEST_TENANTID, TEST_NODEID);
        Mockito.verify(mockEventService, Mockito.times(1)).findEventsByNodeId(TEST_TENANTID, TEST_NODEID);

        Mockito.verify(spyInterceptor).verifyAccessToken(authHeader);
        Mockito.verify(spyInterceptor).interceptCall(ArgumentMatchers.any(ServerCall.class), ArgumentMatchers.any(Metadata.class), ArgumentMatchers.any(ServerCallHandler.class));
    }

    @Test
    void testGetEventsByNodeIdException() throws VerificationException {
        var nodeId = UInt64Value.of(TEST_NODEID);
        var status = Status.fromCode(Status.Code.NOT_FOUND).withDescription("message");
        Mockito.when(mockInventoryClient.getNodeById(TEST_TENANTID, TEST_NODEID)).thenThrow(new StatusRuntimeException(status));

        var stubWithHeader = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()));
        var statusException = Assertions.assertThrows(StatusRuntimeException.class,
            () -> stubWithHeader.getEventsByNodeId(nodeId));

        assertThat(statusException.getStatus().getCode()).isEqualTo(status.getCode());
        assertThat(statusException.getStatus().getDescription()).isEqualTo(status.getDescription());

        Mockito.verify(mockInventoryClient, Mockito.times(1)).getNodeById(TEST_TENANTID, TEST_NODEID);

        Mockito.verify(spyInterceptor).verifyAccessToken(authHeader);
        Mockito.verify(spyInterceptor).interceptCall(ArgumentMatchers.any(ServerCall.class), ArgumentMatchers.any(Metadata.class), ArgumentMatchers.any(ServerCallHandler.class));
    }

    @Test
    void testGetEventsByNodeIdWithoutAuth() throws VerificationException {
        var nodeId = UInt64Value.of(TEST_NODEID);
        var stubWithHeader = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders("Bearer fake")));

        var statusException = Assertions.assertThrows(StatusRuntimeException.class,
            () -> stubWithHeader.getEventsByNodeId(nodeId));

        assertThat(statusException.getStatus().getCode().value()).isEqualTo(Code.UNAUTHENTICATED_VALUE);
        Mockito.verify(spyInterceptor).verifyAccessToken("Bearer fake");
        Mockito.verify(spyInterceptor).interceptCall(ArgumentMatchers.any(ServerCall.class), ArgumentMatchers.any(Metadata.class), ArgumentMatchers.any(ServerCallHandler.class));
    }
}
