package org.opennms.horizon.shared.ipc.grpc.server.manager.rpc;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.rpc.api.RpcModule;
import org.opennms.horizon.shared.ipc.rpc.api.RpcRequest;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationIndependentRpcClient<REQUEST extends RpcRequest, RESPONSE extends RpcResponse> /*implements RpcClient<REQUEST, RESPONSE>*/ {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(LocationIndependentRpcClient.class);

    private Logger log = DEFAULT_LOGGER;

    private MetricRegistry rpcMetrics;
    private long ttl;
    private RpcConnectionTracker rpcConnectionTracker;
    private String serverLocation;

    private final RpcModule<REQUEST, RESPONSE> localModule;
    private final RemoteRegistrationHandler remoteRegistrationHandler; // TODO: injection?


//========================================
// Constructors
//----------------------------------------

    public LocationIndependentRpcClient(
            RpcModule<REQUEST, RESPONSE> localModule,
            RemoteRegistrationHandler remoteRegistrationHandler) {

        this.localModule = localModule;
        this.remoteRegistrationHandler = remoteRegistrationHandler;
    }


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

    //@Override
    public CompletableFuture<RESPONSE> execute(REQUEST request) {
        CompletableFuture<RESPONSE> result;

        if (request.getLocation() == null || Objects.equals(request.getLocation(), serverLocation)) {
            log.debug("executing RPC remotely: remote-location={}; local-location={}", request.getLocation(), serverLocation);
            result = executeLocally(request);
        } else {
            log.debug("executing RPC locally: remote-location={}; local-location={}", request.getLocation(), serverLocation);
            result = executeRemotely(request);
        }

        return result;
    }

//========================================
// Internals
//----------------------------------------

    private CompletableFuture<RESPONSE> executeLocally(REQUEST request) {
        return CompletableFuture.failedFuture(new AbstractMethodError()); // localModule.execute(request);
    }

    /**
     * Calculate the expiration time of the given request.
     *
     * @param request
     * @return
     */
    private long calcuateExpiration(REQUEST request) {
        // Does the request have a custom TTL?
        Long timeToLive = request.getTimeToLiveMs();

        if ((timeToLive == null) || (timeToLive <= 0)) {
            // Not set, or not positive; fallback to the default.
            log.debug("request TTL not set or not positive; using default: request-ttl={}; default={}", timeToLive, ttl);
            timeToLive = ttl;
        }

        // TODO: currentTimeMillis() is susceptible to wall clock adjustments.
        long expirationTime = System.currentTimeMillis() + timeToLive;

        return expirationTime;
    }

    private CompletableFuture<RESPONSE> executeRemotely(REQUEST request) {
        Any marshalRequest = localModule.marshalRequest(request);

        CompletableFuture<RESPONSE> future = new CompletableFuture<>();
        long expirationTime = this.calcuateExpiration(request);
        String rpcId = remoteRegistrationHandler.registerRemoteCall(request, expirationTime, future);

        RpcRequestProto.Builder builder = RpcRequestProto.newBuilder()
                .setRpcId(rpcId)
                .setLocation(request.getLocation())
                .setModuleId(localModule.getId())
                .setPayload(marshalRequest);

        if (!Strings.isNullOrEmpty(request.getSystemId())) {
            builder.setSystemId(request.getSystemId());
        }
        RpcRequestProto requestProto = builder.build();

        boolean succeeded = sendRequest(requestProto);

        if (!succeeded) {
            future.completeExceptionally(new RuntimeException("No minion found at location " + request.getLocation()));
            return future;
        }

        log.debug("RPC request from module: {} with RpcId:{} sent to minion at location {}",
                localModule.getId(),
                rpcId,
                request.getLocation());

        return future;
    }

    private boolean sendRequest(RpcRequestProto requestProto) {
        StreamObserver<RpcRequestProto> rpcHandler;

        // If a specific Minion weas requested, use it
        if (! Strings.isNullOrEmpty(requestProto.getSystemId())) {
            rpcHandler = rpcConnectionTracker.lookupByMinionId(requestProto.getSystemId());
        } else {
            rpcHandler = rpcConnectionTracker.lookupByLocationRoundRobin(requestProto.getLocation());
        }

        if (rpcHandler == null) {
            log.warn("No RPC handlers found for location: location={}; minionId={}", requestProto.getLocation(), requestProto.getSystemId());
            return false;
        }

        try {
            sendRpcRequest(rpcHandler, requestProto);
            return true;
        } catch (Throwable e) {
            log.error("Encountered exception while sending request {}", requestProto, e);
        }

        return false;
    }

    /**
     * Writing message through stream observer is not thread safe.
     */
    private void sendRpcRequest(StreamObserver<RpcRequestProto> rpcHandler, RpcRequestProto rpcMessage) {
        Semaphore connectionSemaphore = rpcConnectionTracker.getConnectionSemaphore(rpcHandler);

        if (connectionSemaphore != null) {
            // StreamObserver.onNext() is NOT thread-safe; use the semaphore to prevent concurrent calls

            // Acquire - note the finally does not go here because a failed call to acquire() must not result in a
            //  call to release()
            try {
                connectionSemaphore.acquire();
            } catch (InterruptedException intExc) {
                throw new RuntimeException("Interrupted while waiting to send request to RPC connection", intExc);
            }

            try {
                // Send
                rpcHandler.onNext(rpcMessage);
            } finally {
                // Release the semaphore
                connectionSemaphore.release();
            }
        }
    }
}
