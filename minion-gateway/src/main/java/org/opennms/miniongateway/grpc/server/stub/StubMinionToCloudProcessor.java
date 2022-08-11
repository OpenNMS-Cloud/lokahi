package org.opennms.miniongateway.grpc.server.stub;

import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class StubMinionToCloudProcessor implements  BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(StubMinionToCloudProcessor.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public void accept(RpcRequestProto rpcRequestProto, StreamObserver<RpcResponseProto> rpcResponseProtoStreamObserver) {
        log.info("Have RPC Request from Minion: system-id={}, location={}, module-id={}, rpc-id={}",
            rpcRequestProto.getSystemId(),
            rpcRequestProto.getLocation(),
            rpcRequestProto.getModuleId(),
            rpcRequestProto.getRpcId());
    }
}
