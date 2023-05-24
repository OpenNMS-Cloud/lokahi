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

package org.opennms.miniongateway.grpc.twin;

import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.*;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.ignite.Ignite;
import org.apache.logging.log4j.util.Strings;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.shared.grpc.common.GrpcIpcUtils;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageFactory;
import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

public class GrpcTwinPublisher extends AbstractTwinPublisher implements OutgoingMessageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcTwinPublisher.class);
    private Multimap<TenantKey, AdapterObserver> sinkStreamsByLocation = LinkedListMultimap.create();
    private Map<TenantKey, AdapterObserver> sinkStreamsBySystemId = new HashMap<>();
    private final ThreadFactory twinRpcThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("twin-rpc-handler-%d")
            .build();
    private final ExecutorService twinRpcExecutor = Executors.newCachedThreadPool(twinRpcThreadFactory);

    public GrpcTwinPublisher(Ignite ignite) {
        super(ignite);
    }

    @Override
    protected void handleSinkUpdate(String location, TwinUpdate sinkUpdate) {
        sendTwinResponseForSink(sinkUpdate.getTenantId(), location, mapTwinResponse(sinkUpdate));
    }

    private synchronized boolean sendTwinResponseForSink(String tenantId, String location, TwinResponseProto twinResponseProto) {
        if (sinkStreamsByLocation.isEmpty()) {
            return false;
        }
        try {
            Object[] diagnosticCtx = {tenantId, twinResponseProto.getConsumerKey(), location, twinResponseProto.getIdentity().getSystemId()};
            if (Strings.isBlank(location)) {
                // theoretical broadcast scenario - no location given, so we send update to all locations
                LOG.debug("Sending sink update for tenant {} with key {} in all locations", tenantId, twinResponseProto.getConsumerKey());
                for (Entry<TenantKey, AdapterObserver> entry : new ArrayList<>(sinkStreamsByLocation.entries())) {
                    if (tenantId.equals(entry.getKey().getTenantId())) {
                        AdapterObserver stream = entry.getValue();
                        try {
                            LOG.debug("Sending sink update for tenant {}, key {}, location {}, system id {}", diagnosticCtx);
                            stream.onNext(twinResponseProto);
                        } catch (StatusRuntimeException e) {
                            LOG.debug("Failed to send sink update for tenant {}, key {}, location {}, system id {}", diagnosticCtx);
                            stream.complete();
                        }
                    }
                }
            } else {
                Collection<AdapterObserver> observers = sinkStreamsByLocation.get(new TenantKey(tenantId, location));
                for (AdapterObserver stream : new ArrayList<>(observers)) {
                    try {
                        LOG.debug("Sending sink update for tenant {}, key {} at location {}", diagnosticCtx);
                        stream.onNext(twinResponseProto);
                    } catch (StatusRuntimeException e) {
                        LOG.debug("Failed to send sink update for tenant {}, key {} at location {}", diagnosticCtx);
                        stream.complete();
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error while sending Twin response for Sink stream", e);
        }
        return true;
    }

    public void start() throws IOException {
        try (MDCCloseable mdc = MDC.putCloseable("prefix", GrpcIpcUtils.LOG_PREFIX)) {
            LOG.info("Activated Twin Service");
        }
    }

    public void close() throws IOException {
        try (MDCCloseable mdc = MDC.putCloseable("prefix", GrpcIpcUtils.LOG_PREFIX)) {
            twinRpcExecutor.shutdown();
            LOG.info("Stopped Twin GRPC Server");
        }
    }

    static class AdapterObserver implements StreamObserver<TwinResponseProto> {

        private final Logger logger = LoggerFactory.getLogger(AdapterObserver.class);
        private final StreamObserver<CloudToMinionMessage> delegate;
        private Runnable completionCallback;

        AdapterObserver(StreamObserver<CloudToMinionMessage> delegate) {
            this.delegate = delegate;
        }

        public void setCompletionCallback(Runnable completion) {
            this.completionCallback = completion;
        }

        @Override
        public void onNext(TwinResponseProto value) {
            delegate.onNext(map(value));
        }

        @Override
        public void onError(Throwable t) {
            logger.warn("Error while processing a stream data", t);
        }

        public void complete() {
            completionCallback.run();
        }

        @Override
        public void onCompleted() {
            completionCallback.run();
        }

        private CloudToMinionMessage map(TwinResponseProto value) {
            return CloudToMinionMessage.newBuilder()
                .setTwinResponse(value)
                .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AdapterObserver)) {
                return false;
            }
            AdapterObserver that = (AdapterObserver) o;
            return Objects.equals(delegate, that.delegate) && Objects.equals(
                completionCallback, that.completionCallback);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate, completionCallback);
        }
    }


    @Override
    public void create(String systemId, String tenantId, String location, StreamObserver<CloudToMinionMessage> streamObserver) {
        TenantKey systemIdKey = new TenantKey(tenantId, systemId);
        TenantKey locationKey = new TenantKey(tenantId, location);
        if (sinkStreamsBySystemId.containsKey(systemIdKey)) {
            StreamObserver<TwinResponseProto> sinkStream = sinkStreamsBySystemId.remove(systemIdKey);
            sinkStreamsByLocation.remove(locationKey, sinkStream);
            sinkStream.onCompleted(); // force termination of session.
        }
        AdapterObserver delegate = new AdapterObserver(streamObserver);
        delegate.setCompletionCallback(() -> {
            sinkStreamsByLocation.remove(locationKey, delegate);
            sinkStreamsBySystemId.remove(systemIdKey);
            // mark stream as done
            streamObserver.onCompleted();
        });
        sinkStreamsByLocation.put(locationKey, delegate);
        sinkStreamsBySystemId.put(systemIdKey, delegate);

        forEachSession(tenantId, ((sessionKey, twinTracker) -> {
            if (sessionKey.location == null || sessionKey.location.equals(locationKey.getKey())) {
                TwinUpdate twinUpdate = new TwinUpdate(sessionKey.key, sessionKey.tenantId, sessionKey.location, twinTracker.getObj());
                twinUpdate.setSessionId(twinTracker.getSessionId());
                twinUpdate.setVersion(twinTracker.getVersion());
                twinUpdate.setPatch(false);
                TwinResponseProto twinResponseProto = mapTwinResponse(twinUpdate);
                delegate.onNext(twinResponseProto);
            }
        }));
    }

}
