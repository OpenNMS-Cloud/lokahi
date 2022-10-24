package org.opennms.horizon.shared.ipc.grpc.server.manager.rpc;


import com.codahale.metrics.MetricRegistry;
import org.opennms.horizon.shared.ipc.grpc.server.manager.LocationIndependentRpcClientFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.rpc.api.RpcModule;
import org.opennms.horizon.shared.ipc.rpc.api.RpcRequest;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponse;

public class LocationIndependentRpcClientFactoryImpl implements LocationIndependentRpcClientFactory {

    private String serverLocation;
    private MetricRegistry rpcMetrics;
    private long ttl;

    private RpcConnectionTracker rpcConnectionTracker;


//========================================
// Getters and Setters
//----------------------------------------


    public String getServerLocation() {
        return serverLocation;
    }

    public void setServerLocation(String serverLocation) {
        this.serverLocation = serverLocation;
    }

    public MetricRegistry getRpcMetrics() {
        return rpcMetrics;
    }

    public void setRpcMetrics(MetricRegistry rpcMetrics) {
        this.rpcMetrics = rpcMetrics;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public RpcConnectionTracker getRpcConnectionTracker() {
        return rpcConnectionTracker;
    }

    public void setRpcConnectionTracker(RpcConnectionTracker rpcConnectionTracker) {
        this.rpcConnectionTracker = rpcConnectionTracker;
    }


//========================================
// Operations
//----------------------------------------

    @Override
    public <REQUEST extends RpcRequest, RESPONSE extends RpcResponse> LocationIndependentRpcClient<REQUEST, RESPONSE>
    createClient(
            RpcModule<REQUEST, RESPONSE> localModule,
            RemoteRegistrationHandler remoteRegistrationHandler
    ) {
        LocationIndependentRpcClient<REQUEST, RESPONSE> result =
                new LocationIndependentRpcClient<>(localModule, remoteRegistrationHandler);

        result.setServerLocation(serverLocation);
        result.setRpcMetrics(rpcMetrics);
        result.setTtl(ttl);
        result.setRpcConnectionTracker(rpcConnectionTracker);

        return result;
    }
}
