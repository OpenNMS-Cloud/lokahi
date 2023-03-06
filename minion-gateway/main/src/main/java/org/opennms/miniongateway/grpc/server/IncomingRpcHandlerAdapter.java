package org.opennms.miniongateway.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingRpcHandlerAdapter implements BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(IncomingRpcHandlerAdapter.class);

    private Logger LOG = DEFAULT_LOGGER;

    private Map<String, ServerHandler> handlers;

    public IncomingRpcHandlerAdapter(List<ServerHandler> handlers) {
        this.handlers = handlers.stream()
            .collect(Collectors.toMap(ServerHandler::getId, Function.identity()));
    }

    @Override
    public void accept(RpcRequestProto request, StreamObserver<RpcResponseProto> responseStream) {
        if (handlers.containsKey(request.getModuleId())) {
            ServerHandler handler = handlers.get(request.getModuleId());
            handler.handle(request).whenComplete((response, error) -> {
                if (error != null) {
                    LOG.error("Exception on RPC: module-id={}", request.getModuleId(), error);
                    responseStream.onError(error);
                    return;
                }
                responseStream.onNext(response);
                responseStream.onCompleted();
            });
        }
    }
}
