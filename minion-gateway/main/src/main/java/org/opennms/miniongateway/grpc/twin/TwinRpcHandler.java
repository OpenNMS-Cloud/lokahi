package org.opennms.miniongateway.grpc.twin;

import com.google.protobuf.Any;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.miniongateway.grpc.server.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwinRpcHandler implements ServerHandler {

    private final Logger logger = LoggerFactory.getLogger(TwinRpcHandler.class);
    private final TwinProvider twinProvider;
    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;
    private final LocationServerInterceptor locationServerInterceptor;
    private Executor twinRpcExecutor = Executors.newSingleThreadScheduledExecutor((runnable) -> new Thread(runnable, "twin-rpc"));

    public TwinRpcHandler(
        TwinProvider twinProvider,
        TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
        LocationServerInterceptor locationServerInterceptor) {
        this.twinProvider = twinProvider;
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
        this.locationServerInterceptor = locationServerInterceptor;
    }

    @Override
    public String getId() {
        return "twin";
    }

    @Override
    public CompletableFuture<RpcResponseProto> handle(RpcRequestProto request) {
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        String locationId = locationServerInterceptor.readCurrentContextLocationId();

        return CompletableFuture.supplyAsync(() -> {
            try {
                TwinRequestProto twinRequest = request.getPayload().unpack(TwinRequestProto.class);
                TwinResponseProto twinResponseProto = twinProvider.getTwinResponse(tenantId, locationId, twinRequest);
                logger.debug("Sent Twin response for key {} at location {}", twinRequest.getConsumerKey(), locationId);

                RpcResponseProto response = RpcResponseProto.newBuilder()
                    .setModuleId("twin")
                    .setRpcId(request.getRpcId())
                    .setIdentity(request.getIdentity())
                    .setPayload(Any.pack(twinResponseProto))
                    .build();

                return response;
            } catch (Exception e) {
                throw new IllegalArgumentException("Exception while processing request", e);
            }
        }, twinRpcExecutor);
    }
}
