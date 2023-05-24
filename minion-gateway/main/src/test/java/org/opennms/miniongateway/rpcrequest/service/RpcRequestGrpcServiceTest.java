package org.opennms.miniongateway.rpcrequest.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.miniongateway.rpcrequest.RpcRequestRouter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

public class RpcRequestGrpcServiceTest {

    private RpcRequestGrpcService target;

    private RuntimeException testException;

    private RpcRequestRouter mockRpcRequestRouter;
    private GrpcIpcServer mockGrpcIpcServer;
    private CompletableFuture<GatewayRpcResponseProto> mockFuture;

    private StreamObserver<GatewayRpcResponseProto> mockResponseStreamObserver;

    @Before
    public void setUp() throws Exception {
        target = new RpcRequestGrpcService();

        testException = new RuntimeException("x-test-exc-x");

        mockRpcRequestRouter = Mockito.mock(RpcRequestRouter.class);
        mockGrpcIpcServer = Mockito.mock(GrpcIpcServer.class);
        mockResponseStreamObserver = Mockito.mock(StreamObserver.class);
        mockFuture = Mockito.mock(CompletableFuture.class);
    }

    @Test
    public void testRequestSuccess() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        GatewayRpcRequestProto requestProto =
            GatewayRpcRequestProto.newBuilder()
                .setIdentity(MinionIdentity.newBuilder().setLocation("x-test-location-x"))
                .build()
            ;
        GatewayRpcResponseProto responseProto =
            GatewayRpcResponseProto.newBuilder()
                .setIdentity(MinionIdentity.newBuilder().setLocation("x-test-location-x"))
                .build()
            ;

        Mockito.when(mockRpcRequestRouter.routeRequest(requestProto)).thenReturn(mockFuture);


        //
        // Execute
        //
        target.setRpcRequestRouter(mockRpcRequestRouter);
        target.setGrpcIpcServer(mockGrpcIpcServer);
        target.start();
        target.request(requestProto, mockResponseStreamObserver);

        // Verify whenComplete() call and execute the completion function

        ArgumentCaptor<BiConsumer<GatewayRpcResponseProto, Throwable>> biConsumerArgumentCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        Mockito.verify(mockFuture).whenComplete(biConsumerArgumentCaptor.capture());

        BiConsumer<GatewayRpcResponseProto, Throwable> processCompletionBiConsumer = biConsumerArgumentCaptor.getValue();
        processCompletionBiConsumer.accept(responseProto, null);

        //
        // Verify the Results
        //
        Mockito.verify(mockResponseStreamObserver).onNext(responseProto);
        Mockito.verify(mockResponseStreamObserver).onCompleted();
    }

    @Test
    public void testRequestException() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        GatewayRpcRequestProto requestProto =
            GatewayRpcRequestProto.newBuilder()
                .setIdentity(MinionIdentity.newBuilder().setLocation("x-test-location-x"))
                .build()
            ;
        RuntimeException testException = new RuntimeException("x-test-exc-x");

        Mockito.when(mockRpcRequestRouter.routeRequest(requestProto)).thenReturn(mockFuture);


        //
        // Execute
        //
        target.setRpcRequestRouter(mockRpcRequestRouter);
        target.setGrpcIpcServer(mockGrpcIpcServer);
        target.start();
        target.request(requestProto, mockResponseStreamObserver);

        // Verify whenComplete() call and execute the completion function

        ArgumentCaptor<BiConsumer<GatewayRpcResponseProto, Throwable>> biConsumerArgumentCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        Mockito.verify(mockFuture).whenComplete(biConsumerArgumentCaptor.capture());

        BiConsumer<GatewayRpcResponseProto, Throwable> processCompletionBiConsumer = biConsumerArgumentCaptor.getValue();
        processCompletionBiConsumer.accept(null, testException);

        //
        // Verify the Results
        //
        Mockito.verify(mockResponseStreamObserver).onError(Mockito.argThat(this::matchesExpectedStatusException));
        Mockito.verify(mockResponseStreamObserver).onCompleted();
    }


    private boolean matchesExpectedStatusException(Throwable thrown) {
        if (thrown instanceof StatusRuntimeException) {

            StatusRuntimeException srExc = (StatusRuntimeException) thrown;

            if (srExc.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                if (srExc.getStatus().getDescription().equals("x-test-exc-x")) {
                    return true;
                }
            }
        }

        return false;
    }
}
