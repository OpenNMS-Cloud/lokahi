package org.opennms.horizon.core.ignite.rpc;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import java.util.UUID;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestProto.Builder;
import org.opennms.horizon.shared.ipc.rpc.api.RequestBuilder;

public class IgniteRequestBuilder implements RequestBuilder {

    private final Builder proto;

    public IgniteRequestBuilder(String module) {
         proto = RpcRequestProto.newBuilder()
             .setModuleId(module);
    }

    @Override
    public RequestBuilder withExpirationTime(long ttl) {
        proto.setExpirationTime(System.currentTimeMillis() + ttl);
        return this;
    }

    @Override
    public RequestBuilder withLocation(String location) {
        proto.setLocation(location);
        return this;
    }

    @Override
    public RequestBuilder withSystemId(String systemId) {
        if (systemId != null) {
            proto.setSystemId(systemId);
        }
        return this;
    }

    @Override
    public RequestBuilder withPayload(Message payload) {
        proto.setPayload(Any.pack(payload));
        return this;
    }

    @Override
    public RpcRequestProto build() {
        return proto.setRpcId(UUID.randomUUID().toString())
            .build();
    }
}
