/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.miniongateway.grpc.twin;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.apache.ignite.Ignite;
import org.apache.logging.log4j.util.Strings;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.shared.grpc.common.GrpcIpcUtils;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageFactory;
import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

public class GrpcTwinPublisher extends AbstractTwinPublisher implements OutgoingMessageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcTwinPublisher.class);
    private Multimap<TenantKey, AdapterObserver> sinkStreamsByLocation = LinkedListMultimap.create();
    private Map<TenantKey, AdapterObserver> sinkStreamsBySystemId = new HashMap<>();
    private final ThreadFactory twinRpcThreadFactory =
            new ThreadFactoryBuilder().setNameFormat("twin-rpc-handler-%d").build();
    private final ExecutorService twinRpcExecutor = Executors.newCachedThreadPool(twinRpcThreadFactory);

    private final Tracer tracer;
    private final boolean debugSpanFullMessage;
    private final boolean debugSpanContent;

    public GrpcTwinPublisher(
            Ignite ignite, final Tracer tracer, boolean debugSpanFullMessage, boolean debugSpanContent) {
        super(ignite);
        this.tracer = tracer;
        this.debugSpanFullMessage = debugSpanFullMessage;
        this.debugSpanContent = debugSpanContent;
    }

    @Override
    protected void handleSinkUpdate(String locationId, TwinUpdate sinkUpdate) {
        sendTwinResponseForSink(sinkUpdate.getTenantId(), locationId, mapTwinResponse(sinkUpdate));
    }

    private synchronized boolean sendTwinResponseForSink(
            String tenantId, String location, TwinResponseProto twinResponseProto) {
        if (sinkStreamsByLocation.isEmpty()) {
            return false;
        }
        try {
            Object[] diagnosticCtx = {tenantId, twinResponseProto.getConsumerKey(), location};
            if (Strings.isBlank(location)) {
                // theoretical broadcast scenario - no location given, so we send update to all locations
                LOG.debug(
                        "Sending sink update for tenant {} with key {} in all locations",
                        tenantId,
                        twinResponseProto.getConsumerKey());
                for (Entry<TenantKey, AdapterObserver> entry : new ArrayList<>(sinkStreamsByLocation.entries())) {
                    if (tenantId.equals(entry.getKey().getTenantId())) {
                        AdapterObserver stream = entry.getValue();
                        try {
                            LOG.debug(
                                    "Sending sink update for tenant {}, key {}, location {}, system id {}",
                                    diagnosticCtx);
                            stream.onNext(twinResponseProto);
                        } catch (StatusRuntimeException e) {
                            LOG.debug(
                                    "Failed to send sink update for tenant {}, key {}, location {}, system id {}",
                                    diagnosticCtx);
                            stream.complete();
                        }
                    }
                }
            } else {
                Collection<AdapterObserver> observers = sinkStreamsByLocation.get(new TenantKey(tenantId, location));
                for (AdapterObserver stream : new ArrayList<>(observers)) {
                    try {
                        try {
                            LOG.debug("Sending sink update for tenant {}, key {} at location {}", diagnosticCtx);
                            stream.onNext(twinResponseProto);
                        } catch (StatusRuntimeException e) {
                            LOG.debug("Failed to send sink update for tenant {}, key {} at location {}", diagnosticCtx);
                            stream.complete();
                        }
                    } catch (Exception e) {
                        LOG.debug("Failed to send sink update for tenant {}, key {} at location {}", diagnosticCtx);
                        LOG.debug("Exception : ", e);
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

    class AdapterObserver implements StreamObserver<TwinResponseProto> {
        private final Logger logger = LoggerFactory.getLogger(AdapterObserver.class);
        private final StreamObserver<CloudToMinionMessage> delegate;
        private Runnable completionCallback;
        // private final Tracer tracer;
        private final Attributes attributes;
        private SpanContext streamSpanContext;

        AdapterObserver(
                StreamObserver<CloudToMinionMessage> delegate,
                Tracer tracer,
                SpanContext streamSpanContext,
                Attributes streamAttributes) {
            this.delegate = delegate;
            // this.tracer = tracer;
            this.streamSpanContext = streamSpanContext;
            this.attributes = streamAttributes;
        }

        public void setCompletionCallback(Runnable completion) {
            this.completionCallback = completion;
        }

        @Override
        public void onNext(TwinResponseProto value) {
            SpanBuilder spanBuilder = tracer.spanBuilder("CloudToMinionMessage send " + value.getConsumerKey())
                    .setSpanKind(SpanKind.PRODUCER)
                    .setAllAttributes(this.attributes)
                    .setAttribute("consumer_key", value.getConsumerKey())
                    .setAttribute("is_patch", value.getIsPatchObject())
                    .setAttribute("session_id", value.getSessionId())
                    .setAttribute("version", value.getVersion());

            if (debugSpanFullMessage) {
                spanBuilder.setAttribute("message", value.toString());
            }
            if (debugSpanContent) {
                spanBuilder.setAttribute("twin_object", value.getTwinObject().toStringUtf8());
            }

            // When we get our original task-set, it is triggered from this span for the incoming connection, so don't
            // add the link
            if (!Span.current().getSpanContext().equals(this.streamSpanContext)) {
                spanBuilder.addLink(this.streamSpanContext);
            }

            final var span = spanBuilder.startSpan();
            try (var ss = span.makeCurrent()) {
                CloudToMinionMessage message = map(value);
                span.setAttribute("size", message.getSerializedSize());
                delegate.onNext(message);
            } catch (Throwable throwable) {
                span.setStatus(StatusCode.ERROR, "Received exception during send: " + throwable);
                span.recordException(throwable);
                throw new UndeclaredThrowableException(throwable);
            } finally {
                span.end();
            }
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
            return CloudToMinionMessage.newBuilder().setTwinResponse(value).build();
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
            return Objects.equals(delegate, that.delegate)
                    && Objects.equals(completionCallback, that.completionCallback);
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate, completionCallback);
        }
    }

    @Override
    public void create(
            String systemId,
            String tenantId,
            String location,
            SpanContext streamSpanContext,
            StreamObserver<CloudToMinionMessage> streamObserver) {
        TenantKey systemIdKey = new TenantKey(tenantId, systemId);
        TenantKey locationKey = new TenantKey(tenantId, location);
        if (sinkStreamsBySystemId.containsKey(systemIdKey)) {
            StreamObserver<TwinResponseProto> sinkStream = sinkStreamsBySystemId.remove(systemIdKey);
            sinkStreamsByLocation.remove(locationKey, sinkStream);
            sinkStream.onCompleted(); // force termination of session.
        }
        var streamAttributes = Attributes.builder()
                .put("user", tenantId)
                .put("location", location)
                .put("systemId", systemId)
                .build();
        AdapterObserver delegate = new AdapterObserver(streamObserver, tracer, streamSpanContext, streamAttributes);
        delegate.setCompletionCallback(() -> {
            sinkStreamsByLocation.remove(locationKey, delegate);
            sinkStreamsBySystemId.remove(systemIdKey);
            // mark stream as done
            streamObserver.onCompleted();
        });
        sinkStreamsByLocation.put(locationKey, delegate);
        sinkStreamsBySystemId.put(systemIdKey, delegate);

        forEachSession(tenantId, ((sessionKey, twinTracker) -> {
            if (sessionKey.locationId == null || sessionKey.locationId.equals(locationKey.getKey())) {
                TwinUpdate twinUpdate = new TwinUpdate(
                        sessionKey.key, sessionKey.tenantId, sessionKey.locationId, twinTracker.getObj());
                twinUpdate.setSessionId(twinTracker.getSessionId());
                twinUpdate.setVersion(twinTracker.getVersion());
                twinUpdate.setPatch(false);
                TwinResponseProto twinResponseProto = mapTwinResponse(twinUpdate);
                delegate.onNext(twinResponseProto);
            }
        }));
    }
}
