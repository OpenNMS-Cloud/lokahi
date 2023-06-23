package org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.impl;

import static org.opennms.horizon.shared.ipc.rpc.api.RpcModule.MINION_HEADERS_MODULE;

import com.google.common.base.Strings;
import io.grpc.stub.StreamObserver;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnection;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC Streaming connection from a Minion.  Note that the Minion identity is expected to be the first message received,
 * so this connection needs to notify the Minion Manager of the minion's identity, and initiate wiring to the internal
 * connection tracker so requests to the minion can find this connection.
 */
public class MinionRpcStreamConnectionImpl implements MinionRpcStreamConnection {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionRpcStreamConnectionImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final StreamObserver<RpcRequestProto> streamObserver;
    private final Consumer<StreamObserver<RpcRequestProto>> onCompleted;
    private final BiConsumer<StreamObserver<RpcRequestProto>, Throwable> onError;
    private final RpcConnectionTracker rpcConnectionTracker;
    private final RpcRequestTracker rpcRequestTracker;
    private final ExecutorService responseHandlerExecutor;
    private final MinionManager minionManager;
    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;
    private final LocationServerInterceptor locationServerInterceptor;

    public MinionRpcStreamConnectionImpl(
            StreamObserver<RpcRequestProto> streamObserver,
            Consumer<StreamObserver<RpcRequestProto>> onCompleted,
            BiConsumer<StreamObserver<RpcRequestProto>, Throwable> onError,
            RpcConnectionTracker rpcConnectionTracker,
            RpcRequestTracker rpcRequestTracker,
            ExecutorService responseHandlerExecutor,
            MinionManager minionManager,
            TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
            LocationServerInterceptor locationServerInterceptor
            ) {

        this.streamObserver = streamObserver;
        this.onCompleted = onCompleted;
        this.onError = onError;
        this.rpcConnectionTracker = rpcConnectionTracker;
        this.rpcRequestTracker = rpcRequestTracker;
        this.responseHandlerExecutor = responseHandlerExecutor;
        this.minionManager = minionManager;
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
        this.locationServerInterceptor = locationServerInterceptor;
    }

    private boolean isMinionIdentityHeaders(RpcResponseProto rpcMessage) {
        return Objects.equals(MINION_HEADERS_MODULE, rpcMessage.getModuleId());
    }

    @Override
    public void handleRpcStreamInboundMessage(RpcResponseProto message) {
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        String location = locationServerInterceptor.readCurrentContextLocationId();

        if (isMinionIdentityHeaders(message)) {
            String systemId = message.getIdentity().getSystemId();

            if (Strings.isNullOrEmpty(location) || Strings.isNullOrEmpty(systemId)) {
                log.error("Invalid metadata received with locationId={}; systemId={}", location, systemId);
                return;
            }

            // Register the Minion
            boolean added = rpcConnectionTracker.addConnection(tenantId, location, systemId, streamObserver);

            if (added) {
                log.info("Added RPC handler for minion: tenantId={}; locationId={}; systemId={};", tenantId, location, systemId);

                // Notify the MinionManager of the addition
                MinionInfo minionInfo = new MinionInfo();
                minionInfo.setTenantId(tenantId);
                minionInfo.setId(systemId);
                minionInfo.setLocation(location);
                minionManager.addMinion(minionInfo);
            }
        } else {
            // Schedule processing of the message which is expected to be a response to a past request sent to the
            //  Minion
            asyncQueueHandleResponse(message);
        }
    }

    @Override
    public void handleRpcStreamInboundError(Throwable thrown) {
        onError.accept(streamObserver, thrown);
    }

    @Override
    public void handleRpcStreamInboundCompleted() {
        onCompleted.accept(streamObserver);
    }

//========================================
// Internals
//----------------------------------------

    private void asyncQueueHandleResponse(RpcResponseProto message) {
        responseHandlerExecutor.execute(() -> syncHandleResponse(message));
    }

    private void syncHandleResponse(RpcResponseProto message) {
        if (Strings.isNullOrEmpty(message.getRpcId())) {
            return;
        }

        // Handle response from the Minion.
        RpcResponseHandler responseHandler = rpcRequestTracker.lookup(message.getRpcId());

        if (responseHandler != null) {
            responseHandler.sendResponse(message);
        } else {
            log.debug("Received a response for request for module: {} with RpcId:{}, but no outstanding request was found with this id." +
                    "The request may have timed out", message.getModuleId(), message.getRpcId());
        }
    }
}
