/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.horizon.minion.ipc.twin.core.internal;


import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.core.ipc.grpc.client.ReconnectStrategy;
import org.opennms.core.ipc.grpc.client.SimpleReconnectStrategy;
import org.opennms.horizon.minion.ipc.twin.common.AbstractTwinSubscriber;
import org.opennms.horizon.minion.ipc.twin.common.TwinRequest;
import org.opennms.horizon.minion.ipc.twin.common.TwinUpdate;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class GrpcTwinSubscriber extends AbstractTwinSubscriber {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcTwinSubscriber.class);
    private static final long RETRIEVAL_TIMEOUT = 1000;
    private static final int TWIN_REQUEST_POOL_SIZE = 100;
    private final int port;
    private final Properties clientProperties;
    private ManagedChannel channel;
    private CloudServiceGrpc.CloudServiceStub asyncStub;
    private AtomicBoolean isShutDown = new AtomicBoolean(false);
    private ResponseHandler responseHandler = new ResponseHandler();
    private final ScheduledExecutorService twinRequestSenderExecutor = Executors.newScheduledThreadPool(TWIN_REQUEST_POOL_SIZE,
            new TwinThreadFactory());
    private ReconnectStrategy reconnectStrategy;

    public GrpcTwinSubscriber(IpcIdentity minionIdentity, ConfigurationAdmin configAdmin, int port) {
        this(minionIdentity, GrpcIpcUtils.getPropertiesFromConfig(configAdmin, GrpcIpcUtils.GRPC_CLIENT_PID), port);
    }

    public GrpcTwinSubscriber(IpcIdentity minionIdentity, Properties clientProperties, int port) {
        super(minionIdentity);
        this.clientProperties = clientProperties;
        this.port = port;
    }

    public void start() throws IOException {
        channel = GrpcIpcUtils.getChannel(clientProperties, this.port);

        asyncStub = CloudServiceGrpc.newStub(channel);
        reconnectStrategy = new SimpleReconnectStrategy(channel, this::sendMinionHeader, () -> {}); // we take no action on disconnect
        reconnectStrategy.activate();
        LOG.info("Started Twin gRPC Subscriber at location {} with systemId {}", getIdentity().getLocation(), getIdentity().getId());
    }

    private synchronized void sendMinionHeader() {
        // Sink stream is unidirectional Response stream from OpenNMS <-> Minion.
        // gRPC Server needs at least one message to initialize the stream
        Identity minionHeader = Identity.newBuilder().setLocation(getIdentity().getLocation())
                                                .setSystemId(getIdentity().getId()).build();
        asyncStub.cloudToMinionMessages(minionHeader, new StreamObserver<>() {
            @Override
            public void onNext(CloudToMinionMessage value) {
                if (value.hasTwinResponse()) {
                    TwinResponseProto twinResponse = value.getTwinResponse();
                    responseHandler.onNext(twinResponse);
                }
            }

            @Override
            public void onError(Throwable t) {
                LOG.warn("Error while processing stream", t);
                responseHandler.onCompleted();
            }

            @Override
            public void onCompleted() {
                LOG.debug("Closed stream");
                reconnectStrategy.activate();
            }
        });
        LOG.info("Registered minion {} in twin service", getIdentity());
    }


    public void close() throws IOException {
        isShutDown.set(true);
        super.close();
        if (channel != null) {
            channel.shutdown();
        }
        twinRequestSenderExecutor.shutdown();
    }

    @Override
    protected void sendRpcRequest(TwinRequest twinRequest) {
        try {
            TwinRequestProto twinRequestProto = mapTwinRequestToProto(twinRequest);
            // Send RPC Request asynchronously.
            CompletableFuture.runAsync(() -> retrySendRpcRequest(twinRequestProto), twinRequestSenderExecutor);
        } catch (Exception e) {
            LOG.error("Exception while sending request with key {}", twinRequest.getKey());
        }
    }

    private void retrySendRpcRequest(TwinRequestProto twinRequestProto) {
        // We can only send RPC If channel is active and RPC stream is not in error.
        // Schedule sending RPC request with given retrieval timeout until it succeeds.
        scheduleWithDelayUntilFunctionSucceeds(twinRequestSenderExecutor, this::sendTwinRpcRequest, RETRIEVAL_TIMEOUT, twinRequestProto);
    }

    private <T> void scheduleWithDelayUntilFunctionSucceeds(ScheduledExecutorService executorService,
                                                            Function<T, Boolean> function,
                                                            long delayInMsec,
                                                            T obj) {
        boolean succeeded = function.apply(obj);
        if (!succeeded) {
            do {
                ScheduledFuture<Boolean> future = executorService.schedule(() -> function.apply(obj), delayInMsec, TimeUnit.MILLISECONDS);
                try {
                    succeeded = future.get();
                    if (succeeded) {
                        break;
                    }
                } catch (Exception e) {
                    // It's likely that error persists, bail out
                    succeeded = true;
                    LOG.warn("Error while attempting to schedule the task", e);
                }
            } while (!succeeded || !isShutDown.get());
        }
    }

    private synchronized boolean sendTwinRpcRequest(TwinRequestProto twinRequestProto) {
        if (asyncStub == null) {
            return false;
        }

        String rpcId = UUID.randomUUID().toString();
        RpcRequestProto rpcRequestProto = RpcRequestProto.newBuilder()
            .setRpcContent(twinRequestProto.toByteString())
            .setModuleId("twin")
            .setRpcId(rpcId)
            .build();
        asyncStub.minionToCloudRPC(rpcRequestProto, new StreamObserver<>() {
            @Override
            public void onNext(RpcResponseProto value) {
                try {
                    TwinResponseProto twinResponseProto = TwinResponseProto.parseFrom(value.getRpcContent());
                    responseHandler.onNext(twinResponseProto);
                } catch (InvalidProtocolBufferException e) {
                    LOG.error("Error while requesting twin {}, received answer {}", twinRequestProto, value);
                }
            }

            @Override
            public void onError(Throwable t) {
                LOG.warn("Error while requesting twin {}, received answer {}", twinRequestProto, t);
                reconnectStrategy.activate();
            }

            @Override
            public void onCompleted() {
                LOG.debug("Closed reply stream");
                reconnectStrategy.activate();
            }
        });
        return true;
    }

    private class ResponseHandler implements StreamObserver<TwinResponseProto> {

        @Override
        public void onNext(TwinResponseProto twinResponseProto) {
            try {
                TwinUpdate twinUpdate = mapTwinResponseToProto(twinResponseProto.toByteArray());
                accept(twinUpdate);
            } catch (Exception e) {
                LOG.error("Exception while processing twin update for key {} ", twinResponseProto.getConsumerKey(), e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            LOG.error("Error in Twin streaming", throwable);
            reconnectStrategy.activate();
        }

        @Override
        public void onCompleted() {
            LOG.error("Closing Twin Response Handler");
            reconnectStrategy.activate();
        }

    }

    static class TwinThreadFactory implements ThreadFactory {
        private final static AtomicInteger COUNTER = new AtomicInteger();
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "twin-request-sender-" + COUNTER.incrementAndGet());
        }
    }
}
