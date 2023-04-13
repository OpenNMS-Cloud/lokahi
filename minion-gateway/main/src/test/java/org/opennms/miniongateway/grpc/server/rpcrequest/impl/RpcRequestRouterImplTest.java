package org.opennms.miniongateway.grpc.server.rpcrequest.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.miniongateway.grpc.server.rpcrequest.RouterTaskData;
import org.opennms.miniongateway.grpc.server.rpcrequest.RpcRequestRouterIgniteTask;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class RpcRequestRouterImplTest {

    private RpcRequestRouterImpl target;

    private Logger mockLogger;
    private Ignite mockIgnite;
    private IgniteCompute mockIgniteCompute;
    private RpcRequestRouterIgniteTask mockRpcRequestRouterIgniteTask;
    private ComputeTaskFuture mockComputeTaskFuture;
    private IgniteFuture mockIgniteFuture;
    private TenantIDGrpcServerInterceptor mockTenantIDGrpcServerInterceptor;

    @Before
    public void setUp() throws Exception {
        target = new RpcRequestRouterImpl();

        mockLogger = Mockito.mock(Logger.class);
        mockIgnite = Mockito.mock(Ignite.class);
        mockIgniteCompute = Mockito.mock(IgniteCompute.class);
        mockRpcRequestRouterIgniteTask = Mockito.mock(RpcRequestRouterIgniteTask.class);
        mockComputeTaskFuture = Mockito.mock(ComputeTaskFuture.class);
        mockIgniteFuture = Mockito.mock(IgniteFuture.class);
        mockTenantIDGrpcServerInterceptor = Mockito.mock(TenantIDGrpcServerInterceptor.class);

        Mockito.when(mockIgnite.compute()).thenReturn(mockIgniteCompute);
        Mockito.when(mockIgniteCompute.executeAsync(Mockito.same(mockRpcRequestRouterIgniteTask), Mockito.any(RouterTaskData.class))).thenReturn(mockComputeTaskFuture);
        Mockito.when(mockTenantIDGrpcServerInterceptor.readCurrentContextTenantId()).thenReturn("x-tenant-id-x");
    }

    @Test
    public void testRouteRequest() {
        //
        // Setup Test Data and Interactions
        //
        GatewayRpcRequestProto testRequest =
            GatewayRpcRequestProto.newBuilder()
                .setIdentity(MinionIdentity.newBuilder().setLocation("x-test-location-x"))
                .setRpcId("x-rpc-id-x")
                .build()
            ;
        GatewayRpcResponseProto rpcResponseProto =
            GatewayRpcResponseProto.newBuilder()
                .setIdentity(MinionIdentity.newBuilder().setLocation("x-test-response-location-x"))
                .build()
            ;
        byte[] responseBytes = rpcResponseProto.toByteArray();

        Mockito.when(mockIgniteFuture.get()).thenReturn(responseBytes);
        Mockito.when(mockIgniteFuture.isDone()).thenReturn(true);


        //
        // Execute
        //
        target.setIgnite(mockIgnite);
        target.setRpcRequestRouterIgniteTask(mockRpcRequestRouterIgniteTask);
        CompletableFuture<GatewayRpcResponseProto> completableFuture = target.routeRequest(testRequest);

        // Verify listen() call and execute the completion function
        ArgumentCaptor<IgniteInClosure> runnableArgumentCaptor = ArgumentCaptor.forClass(IgniteInClosure.class);
        Mockito.verify(mockComputeTaskFuture).listen(runnableArgumentCaptor.capture());

        IgniteInClosure igniteInClosure = runnableArgumentCaptor.getValue();
        igniteInClosure.apply(mockIgniteFuture);


        //
        // Verify the Results
        //
        GatewayRpcResponseProto result = completableFuture.getNow(null);
        assertEquals("x-test-response-location-x", result.getIdentity().getLocation());
    }

    @Test
    public void testExceptionFromFuture() {
        //
        // Setup Test Data and Interactions
        //
        GatewayRpcRequestProto testRequest =
            GatewayRpcRequestProto.newBuilder()
                .setIdentity(MinionIdentity.newBuilder().setLocation("x-test-location-x"))
                .setRpcId("x-rpc-id-x")
                .build()
            ;

        RuntimeException testException = new RuntimeException("x-test-exception-x");

        Mockito.when(mockIgniteFuture.get()).thenThrow(testException);
        Mockito.when(mockIgniteFuture.isDone()).thenReturn(true);


        //
        // Execute
        //
        target.setIgnite(mockIgnite);
        target.setRpcRequestRouterIgniteTask(mockRpcRequestRouterIgniteTask);
        CompletableFuture<GatewayRpcResponseProto> completableFuture = target.routeRequest(testRequest);

        // Verify listen() call and execute the completion function
        ArgumentCaptor<IgniteInClosure> runnableArgumentCaptor = ArgumentCaptor.forClass(IgniteInClosure.class);
        Mockito.verify(mockComputeTaskFuture).listen(runnableArgumentCaptor.capture());

        IgniteInClosure igniteInClosure = runnableArgumentCaptor.getValue();
        igniteInClosure.apply(mockIgniteFuture);


        //
        // Verify the Results
        //
        try {
            GatewayRpcResponseProto result = completableFuture.getNow(null);
            fail("missing expected exception");
        } catch (Exception exc) {
            assertSame(testException, exc.getCause());
        }
    }

    @Test
    public void testInvalidProtocolBufferExceptionFromFuture() {
        //
        // Setup Test Data and Interactions
        //
        GatewayRpcRequestProto testRequest =
            GatewayRpcRequestProto.newBuilder()
                .setIdentity(MinionIdentity.newBuilder().setLocation("x-test-location-x"))
                .setRpcId("x-rpc-id-x")
                .build()
            ;

        InvalidProtocolBufferException testException = new InvalidProtocolBufferException("x-test-exception-x");

        Mockito.when(mockIgniteFuture.get()).thenReturn("garbage".getBytes(StandardCharsets.UTF_8));
        Mockito.when(mockIgniteFuture.isDone()).thenReturn(true);


        //
        // Execute
        //
        target.setLog(mockLogger);
        target.setIgnite(mockIgnite);
        target.setRpcRequestRouterIgniteTask(mockRpcRequestRouterIgniteTask);
        CompletableFuture<GatewayRpcResponseProto> completableFuture = target.routeRequest(testRequest);

        // Verify listen() call and execute the completion function
        ArgumentCaptor<IgniteInClosure> runnableArgumentCaptor = ArgumentCaptor.forClass(IgniteInClosure.class);
        Mockito.verify(mockComputeTaskFuture).listen(runnableArgumentCaptor.capture());

        IgniteInClosure igniteInClosure = runnableArgumentCaptor.getValue();
        igniteInClosure.apply(mockIgniteFuture);


        //
        // Verify the Results
        //
        try {
            GatewayRpcResponseProto result = completableFuture.getNow(null);
            fail("missing expected exception");
        } catch (Exception exc) {
            Mockito.verify(mockLogger).error("failed to parse RPC response", exc.getCause());
        }
    }
}
