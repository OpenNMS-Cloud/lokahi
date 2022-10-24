package org.opennms.horizon.minion.icmp.ipc.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.grpc.ping.contract.PingRequest;
import org.opennms.horizon.grpc.ping.contract.PingResponse;
import org.opennms.horizon.shared.ipc.rpc.api.minion.RpcHandler;
import org.opennms.horizon.shared.icmp.PingerFactory;

public class PingRpcHandler implements RpcHandler<PingRequest, PingResponse> {

    public static final String RPC_MODULE_ID = "PING";

    private final PingerFactory pingerFactory;

    public PingRpcHandler(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;
    }

    @Override
    public CompletableFuture<PingResponse> execute(PingRequest request) {
        try {
            InetAddress address = InetAddress.getByName(request.getInetAddress());
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return pingerFactory.getInstance().ping(address);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(ttl -> PingResponse.newBuilder()
                .setRtt(ttl.doubleValue())
                .build());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String getId() {
        return RPC_MODULE_ID;
    }

    @Override
    public PingRequest unmarshal(RpcRequestProto request) {
        try {
            return request.getPayload().unpack(PingRequest.class);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
