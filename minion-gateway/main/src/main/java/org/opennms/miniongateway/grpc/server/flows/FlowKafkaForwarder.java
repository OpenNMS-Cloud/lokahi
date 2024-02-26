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
package org.opennms.miniongateway.grpc.server.flows;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentLogMapper;
import org.opennms.horizon.shared.grpc.interceptor.MeteringServerInterceptor;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Forwarder of Flow messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class FlowKafkaForwarder implements MessageConsumer<FlowDocumentLog, FlowDocumentLog> {
    public static final String DEFAULT_FLOW_RESULTS_TOPIC = "flows";

    public static final String FLOW_DOCUMENT_TAG = "FlowDocument";
    public static final String FLOW_DOCUMENT_LOG_TAG = "FlowDocumentLog";
    private final SinkMessageKafkaPublisher<FlowDocumentLog, TenantLocationSpecificFlowDocumentLog> kafkaPublisher;
    private final Counter flowCounter;
    private final Counter flowLogCounter;

    @Autowired
    public FlowKafkaForwarder(
            SinkMessageKafkaPublisherFactory messagePublisherFactory,
            TenantLocationSpecificFlowDocumentLogMapper mapper,
            @Value("${flow.results.kafka-topic:" + DEFAULT_FLOW_RESULTS_TOPIC + "}") String kafkaTopic,
            MeterRegistry registry) {
        this.kafkaPublisher = messagePublisherFactory.create(mapper::mapBareToTenanted, kafkaTopic);
        Objects.requireNonNull(registry);
        flowCounter = registry.counter(
                FlowKafkaForwarder.class.getName(), MeteringServerInterceptor.SERVICE_TAG_NAME, FLOW_DOCUMENT_TAG);
        flowLogCounter = registry.counter(
                FlowKafkaForwarder.class.getName(), MeteringServerInterceptor.SERVICE_TAG_NAME, FLOW_DOCUMENT_LOG_TAG);
    }

    @Override
    public SinkModule<FlowDocumentLog, FlowDocumentLog> getModule() {
        return new org.opennms.miniongateway.grpc.server.flows.FlowSinkModule();
    }

    @Override
    public void handleMessage(FlowDocumentLog messageLog) {
        flowCounter.increment(messageLog.getMessageCount());
        flowLogCounter.increment();
        this.kafkaPublisher.send(messageLog);
    }
}
