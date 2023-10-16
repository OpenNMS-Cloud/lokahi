package org.opennms.horizon.events.grpc.client;

import com.google.protobuf.Int64Value;
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
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.io.IOException;

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
    private static NodeServiceGrpc.NodeServiceImplBase mockNodeService;

    private final String accessToken = "test-token";

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockInterceptor = new MockServerInterceptor();

        mockNodeService = mock(NodeServiceGrpc.NodeServiceImplBase.class, delegatesTo(
            new NodeServiceGrpc.NodeServiceImplBase() {
                @Override
                public void getNodeById(Int64Value request, StreamObserver<NodeDTO> responseObserver) {
                    responseObserver.onNext(NodeDTO.newBuilder()
                        .setId(request.getValue()).build());
                    responseObserver.onCompleted();
                }
            }));

        grpcCleanUp.register(InProcessServerBuilder.forName("InventoryClientTest").intercept(mockInterceptor)
            .addService(mockNodeService)
            .directExecutor().build().start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("InventoryClientTest").directExecutor().build());
        client = new InventoryClient(channel, 5000);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockNodeService);
        reset(mockNodeService);
        mockInterceptor.reset();
    }

    @Test
    void testGetNodeById() {
        long nodeId = 100L;
        ArgumentCaptor<Int64Value> captor = ArgumentCaptor.forClass(Int64Value.class);
        NodeDTO result = client.getNodeById("tenantId", nodeId);
        assertThat(result).isNotNull();
        verify(mockNodeService).getNodeById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(nodeId);
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
