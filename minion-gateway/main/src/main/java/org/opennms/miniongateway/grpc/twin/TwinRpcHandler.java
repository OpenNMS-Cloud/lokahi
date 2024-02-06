package org.opennms.miniongateway.grpc.twin;

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

import com.google.protobuf.Any;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class TwinRpcHandler implements ServerHandler {

    private final Logger logger = LoggerFactory.getLogger(TwinRpcHandler.class);

    private final TwinProvider twinProvider;
    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;
    private final LocationServerInterceptor locationServerInterceptor;
    private final Tracer tracer;
    private final boolean debugSpanFullMessage;
    private final boolean debugSpanContent;
    private Executor twinRpcExecutor = Executors.newSingleThreadScheduledExecutor((runnable) -> new Thread(runnable, "twin-rpc"));

    public TwinRpcHandler(
        TwinProvider twinProvider,
        TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
        LocationServerInterceptor locationServerInterceptor,
        final Tracer tracer, boolean debugSpanFullMessage, boolean debugSpanContent) {
        this.twinProvider = twinProvider;
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
        this.locationServerInterceptor = locationServerInterceptor;
        this.tracer = tracer;
        this.debugSpanFullMessage = debugSpanFullMessage;
        this.debugSpanContent = debugSpanContent;
    }

    @Override
    public String getId() {
        return "twin";
    }

    @Override
    public CompletableFuture<RpcResponseProto> handle(RpcRequestProto request) {
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        String locationId = locationServerInterceptor.readCurrentContextLocationId();

        var attributesBuilder = Attributes.builder()
            .put("user", tenantId)
            .put("location", locationId);
        if (request.hasIdentity()) {
            attributesBuilder.put("systemId", request.getIdentity().getSystemId());
        }
        var attributes = attributesBuilder.build();

        var span = Span.current();
        span.updateName("minion.CloudService/MinionToCloudRPC " + request.getModuleId());
        span
            .setAllAttributes(attributes)
            .setAttribute("request_size", request.getSerializedSize())
            .setAttribute("rpcId", request.getRpcId())
            .setAttribute("expiration", request.getExpirationTime())
            .setAttribute("moduleId", request.getModuleId());

        if (debugSpanFullMessage) {
            span.setAttribute("request", request.toString());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                TwinRequestProto twinRequest = request.getPayload().unpack(TwinRequestProto.class);

                span.updateName("minion.CloudService/MinionToCloudRPC " + request.getModuleId() + " " + twinRequest.getConsumerKey());
                span.setAttribute("consumerKey", twinRequest.getConsumerKey());
                if (debugSpanContent) {
                    span.setAttribute("request_payload", twinRequest.toString());
                }

                final TwinResponseProto twinResponseProto = twinProvider.getTwinResponse(tenantId, locationId, twinRequest);

                RpcResponseProto response = RpcResponseProto.newBuilder()
                    .setModuleId("twin")
                    .setRpcId(request.getRpcId())
                    .setIdentity(request.getIdentity())
                    .setPayload(Any.pack(twinResponseProto))
                    .build();

                span.setAttribute("response_size", response.getSerializedSize());
                if (debugSpanFullMessage) {
                    span.setAttribute("response", response.toString());
                }
                if (debugSpanContent && response.hasPayload()) {
                    span.setAttribute("response_payload", response.getPayload().toString());
                }

                logger.debug("Sending Twin response for key {} at location {}", twinRequest.getConsumerKey(), locationId);
                return response;
            } catch (Exception e) {
                throw new IllegalArgumentException("Exception while processing request", e);
            }
        }, twinRpcExecutor);
    }
}
