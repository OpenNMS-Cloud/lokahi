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


import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.minion.ipc.twin.common.AbstractTwinSubscriber;
import org.opennms.horizon.minion.ipc.twin.common.TwinRequest;
import org.opennms.horizon.minion.ipc.twin.common.TwinUpdate;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.rpc.api.minion.ClientRequestDispatcher;
import org.opennms.horizon.shared.ipc.rpc.api.minion.CloudMessageReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcTwinSubscriber extends AbstractTwinSubscriber implements CloudMessageReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcTwinSubscriber.class);
    private static final long RETRIEVAL_TIMEOUT = 1000;
    private static final int TWIN_REQUEST_POOL_SIZE = 100;
    private final ClientRequestDispatcher requestDispatcher;

    private AtomicBoolean isShutDown = new AtomicBoolean(false);
    private final ScheduledExecutorService twinRequestSenderExecutor = Executors.newScheduledThreadPool(TWIN_REQUEST_POOL_SIZE,
            new TwinThreadFactory());

    public GrpcTwinSubscriber(IpcIdentity minionIdentity, ClientRequestDispatcher requestDispatcher) {
        super(minionIdentity);
        this.requestDispatcher = requestDispatcher;
    }

    public void close() throws IOException {
        super.close();
        isShutDown.set(true);
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
        String rpcId = UUID.randomUUID().toString();
        RpcRequestProto rpcRequestProto = RpcRequestProto.newBuilder()
            .setSystemId(getIdentity().getId())
            .setLocation(getIdentity().getLocation())
            .setPayload(Any.pack(twinRequestProto))
            .setModuleId("twin")
            .setRpcId(rpcId)
            .build();

        requestDispatcher.call(rpcRequestProto).whenComplete((response, error) -> {
            if (error != null) {
                return;
            }
            if (rpcId.equals(response.getRpcId())) {
                try {
                    TwinResponseProto twinResponseProto = response.getPayload().unpack(TwinResponseProto.class);
                    handle(twinResponseProto);
                } catch (InvalidProtocolBufferException e) {

                }
            }
        });

        return true;
    }

    @Override
    public void handle(Message message) {
        CloudToMinionMessage cloudMessage = (CloudToMinionMessage) message;
        if (cloudMessage.hasTwinResponse()) {
            handle(cloudMessage.getTwinResponse());
        }
    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof CloudToMinionMessage;
    }

    private void handle(TwinResponseProto twinResponseProto) {
        try {
            TwinUpdate twinUpdate = mapTwinResponseToProto(twinResponseProto.toByteArray());
            accept(twinUpdate);
        } catch (Exception e) {
            LOG.error("Exception while processing twin update for key {} ", twinResponseProto.getConsumerKey(), e);
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
