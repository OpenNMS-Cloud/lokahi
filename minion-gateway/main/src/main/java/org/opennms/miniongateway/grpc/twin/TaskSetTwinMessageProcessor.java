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

import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.List;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskSetTwinMessageProcessor implements OutgoingMessageHandler {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetTwinMessageProcessor.class);

    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;
    private final LocationServerInterceptor locationServerInterceptor;

    private final List<OutgoingMessageFactory> outgoingMessageFactoryList;
    private final boolean debugSpanFullMessage;
    private final boolean debugSpanContent;

    private Logger log = DEFAULT_LOGGER;

    public TaskSetTwinMessageProcessor(
            TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
            LocationServerInterceptor locationServerInterceptor,
            List<OutgoingMessageFactory> outgoingMessageFactoryList,
            boolean debugSpanFullMessage,
            boolean debugSpanContent) {
        this.outgoingMessageFactoryList = outgoingMessageFactoryList;
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
        this.locationServerInterceptor = locationServerInterceptor;
        this.debugSpanFullMessage = debugSpanFullMessage;
        this.debugSpanContent = debugSpanContent;
    }

    /**
     *
     * @param minionHeader Identity message received from the Minion as the first (and only) message after it connects.
     * @param cloudToMinionMessageStreamObserver observer that handles this stream
     * @param streamSpan the Span for the parent stream for this message, used relate later cloud message Spans back
     *                   to the stream Span and to add identity attributes to the stream Span.
     *                   This needs to be passed into this method because a new span is started for this
     *                   method using @WithSpan and at that point we lose visibility to the stream Span.
     */
    @Override
    @WithSpan(value = "CloudToMinionMessage Minion identity received", kind = SpanKind.CONSUMER)
    public void handleOutgoingStream(
            Identity minionHeader,
            StreamObserver<CloudToMinionMessage> cloudToMinionMessageStreamObserver,
            Span streamSpan) {
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        String location = locationServerInterceptor.readCurrentContextLocationId();
        String systemId = minionHeader.getSystemId();

        var attributes = Attributes.builder()
                .put("user", tenantId)
                .put("location", location)
                .put("systemId", systemId)
                .build();

        var span = Span.current();

        // We make sure the identity attributes are set both on the streamSpanContext and our current span.
        streamSpan.setAllAttributes(attributes);
        span.setAllAttributes(attributes);

        span.setAttribute("size", minionHeader.getSerializedSize());
        if (debugSpanFullMessage) {
            span.setAttribute("message", minionHeader.toByteString().toStringUtf8());
        }
        // No debugSpanContent because it's just the systemId.

        log.info(
                "Received initial CloudToMinionMessages Identity message for Minion: tenant-id: {}; location={}; system-id={}",
                tenantId,
                location,
                systemId);

        for (OutgoingMessageFactory outgoingMessageFactory : outgoingMessageFactoryList) {
            // streamObserver.accept(identity, cloudToMinionMessageStreamObserver);
            outgoingMessageFactory.create(
                    systemId, tenantId, location, streamSpan.getSpanContext(), cloudToMinionMessageStreamObserver);
        }
    }
}
