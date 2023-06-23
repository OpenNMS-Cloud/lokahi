package org.opennms.horizon.shared.ipc.rpc.api;

import com.google.protobuf.Message;
import org.opennms.cloud.grpc.minion.RpcRequestProto;

/**
 * Builder which keeps care of null safety for constructed {@link RpcRequestProto} instances.
 *
 * Each call to {@link #build()} method will produce new instance of request with unique identifier.
 */
public interface RequestBuilder {

    RequestBuilder withExpirationTime(long ttl);
    RequestBuilder withLocation(String location);
    RequestBuilder withSystemId(String systemId);
    RequestBuilder withPayload(Message payload);
    RpcRequestProto build();

}
