package org.opennms.horizon.core.ignite.rpc;

import com.google.protobuf.Message;
import org.apache.ignite.client.IgniteClient;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;

public class IgniteRpcClientFactory implements RpcClientFactory {

    private IgniteClient client;

    public IgniteRpcClientFactory(IgniteClient client) {
        this.client = client;
    }

    @Override
    public <T extends Message> RpcClient<T>  getClient(Deserializer<T> deserializer) {
        return new IgniteRpcClient<>(client, deserializer);
    }

    @Override
    public RpcClient<RpcResponseProto> getClient() {
        return new IgniteRpcClient<>(client, response -> response);
    }
}
