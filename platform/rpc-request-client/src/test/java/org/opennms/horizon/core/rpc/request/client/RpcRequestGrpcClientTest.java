package org.opennms.horizon.core.rpc.request.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

public class RpcRequestGrpcClientTest {

    private RpcRequestGrpcClient target;

    private NettyChannelBuilder mockNettyChannelBuilder;
    private RpcRequestServiceGrpc.RpcRequestServiceFutureStub mockRpcRequestServiceFutureStub;
    private ManagedChannel mockChannel;
    private ListenableFuture mockListenableFuture;
    private Logger mockLogger;

    private RpcRequestProto testRequest;
    private RpcResponseProto testResponse;

    @Before
    public void init() throws ExecutionException, InterruptedException {
        testRequest = RpcRequestProto.newBuilder().setLocation("x-test-location-x").setRpcId("x-rpc-id-x").build();
        testResponse = RpcResponseProto.newBuilder().setLocation("x-test-location-x").setRpcId("x-rpc-id-x").build();

        target = new RpcRequestGrpcClient();

        // TODO: why is the compiler complaining without these casts?
        target.setNettyChannelBuilderForAddressFunction((BiFunction<String, Integer, NettyChannelBuilder>) this::testNettyChannelBuilderForAddressFunction);
        target.setRpcRequestServiceNewFutureStubFunction((Function<Channel, RpcRequestServiceGrpc.RpcRequestServiceFutureStub>) this::testRpcRequestServiceNewFutureStubFunction);

        mockNettyChannelBuilder = Mockito.mock(NettyChannelBuilder.class);
        mockRpcRequestServiceFutureStub = Mockito.mock(RpcRequestServiceGrpc.RpcRequestServiceFutureStub.class);
        mockChannel = Mockito.mock(ManagedChannel.class);
        mockListenableFuture = Mockito.mock(ListenableFuture.class);
        mockLogger = Mockito.mock(Logger.class);

        Mockito.when(mockNettyChannelBuilder.keepAliveWithoutCalls(true)).thenReturn(mockNettyChannelBuilder);
        Mockito.when(mockNettyChannelBuilder.maxInboundMessageSize(RpcRequestGrpcClientFactory.DEFAULT_MAX_MESSAGE_SIZE)).thenReturn(mockNettyChannelBuilder);
        Mockito.when(mockNettyChannelBuilder.usePlaintext()).thenReturn(mockNettyChannelBuilder);

        Mockito.when(mockNettyChannelBuilder.build()).thenReturn(mockChannel);
    }

    @Test
    public void testExecuteDefaultDeserializer() throws ExecutionException, InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockRpcRequestServiceFutureStub.request(testRequest)).thenReturn(mockListenableFuture);
        Mockito.when(mockListenableFuture.get()).thenReturn(testResponse);

        //
        // Execute
        //
        target.init();
        CompletableFuture result = target.execute(testRequest);

        //
        // Verify the Results
        //
        assertNotNull(result);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockListenableFuture).addListener(runnableArgumentCaptor.capture(), Mockito.any(Executor.class));

        Runnable processRequestCompletionRunnable = runnableArgumentCaptor.getValue();
        processRequestCompletionRunnable.run();

        assertSame(testResponse, result.getNow(null));
    }

    @Test
    public void testExecuteCustomDeserializer() throws ExecutionException, InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        Message mockMessage = Mockito.mock(Message.class);
        RpcClientFactory.Deserializer<Message> testDeserializer = (in) -> mockMessage;
        Mockito.when(mockRpcRequestServiceFutureStub.request(testRequest)).thenReturn(mockListenableFuture);
        Mockito.when(mockListenableFuture.get()).thenReturn(testResponse);

        //
        // Execute
        //
        target.setDeserializer(testDeserializer);
        target.init();
        CompletableFuture result = target.execute(testRequest);

        // Verify addListener() call and execute the completion function

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockListenableFuture).addListener(runnableArgumentCaptor.capture(), Mockito.any(Executor.class));

        // Execute the completion function
        Runnable processRequestCompletionRunnable = runnableArgumentCaptor.getValue();
        processRequestCompletionRunnable.run();


        //
        // Verify the Results
        //
        assertNotNull(result);
        assertSame(mockMessage, result.getNow(null));
    }

    @Test
    public void testTlsEnabled() throws ExecutionException, InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockRpcRequestServiceFutureStub.request(testRequest)).thenReturn(mockListenableFuture);
        Mockito.when(mockListenableFuture.get()).thenReturn(testResponse);

        //
        // Execute
        //
        target.setTlsEnabled(true);

        RuntimeException resultException = null;
        try {
            target.init();
        } catch (RuntimeException rtExc) {
            resultException = rtExc;
        }

        //
        // Verify the Results
        //
        assertNotNull(resultException);
        assertEquals("TLS NOT YET IMPLEMENTED", resultException.getMessage());
    }

    @Test
    public void testExceptionOnStartRequest() {
        //
        // Setup Test Data and Interactions
        //
        RuntimeException testException = new RuntimeException("x-test-request-exception-x");
        Mockito.when(mockRpcRequestServiceFutureStub.request(testRequest)).thenThrow(testException);

        //
        // Execute
        //
        target.setLog(mockLogger);
        target.init();
        CompletableFuture resultFuture = target.execute(testRequest);

        //
        // Verify the Results
        //
        assertNotNull(resultFuture);
        try {
            resultFuture.getNow(null);
            fail("missing expected exception");
        } catch (Exception exc) {
            // The real exception is wrapped, so compare against the cause
            assertSame(testException, exc.getCause());
        }
    }

    @Test
    public void testExceptionOnGrpcListenerGet() throws ExecutionException, InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        RuntimeException testException = new RuntimeException("x-test-get-exception-x");
        Mockito.when(mockRpcRequestServiceFutureStub.request(testRequest)).thenReturn(mockListenableFuture);
        Mockito.when(mockListenableFuture.get()).thenThrow(testException);

        //
        // Execute
        //
        target.setLog(mockLogger);
        target.init();
        CompletableFuture resultFuture = target.execute(testRequest);

        // Verify addListener() call and execute the completion function

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockListenableFuture).addListener(runnableArgumentCaptor.capture(), Mockito.any(Executor.class));

        Runnable processRequestCompletionRunnable = runnableArgumentCaptor.getValue();
        processRequestCompletionRunnable.run();


        //
        // Verify the Results
        //
        assertNotNull(resultFuture);
        try {
            resultFuture.getNow(null);
            fail("missing expected exception");
        } catch (Exception exc) {
            // The real exception is wrapped twice, so compare against the cause of the cause
            assertSame(testException, exc.getCause().getCause());
        }
    }

//========================================
//
//----------------------------------------

    private NettyChannelBuilder testNettyChannelBuilderForAddressFunction(String host, int port) {
        return mockNettyChannelBuilder;
    }

    private RpcRequestServiceGrpc.RpcRequestServiceFutureStub testRpcRequestServiceNewFutureStubFunction(Channel channel) {
        return mockRpcRequestServiceFutureStub;
    }
}
