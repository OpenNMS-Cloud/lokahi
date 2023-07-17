/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.miniongateway.grpc.server.flows;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

import java.util.Objects;

/**
 * Forwarder of Flow messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class FlowKafkaForwarder implements MessageConsumer<FlowDocumentLog, FlowDocumentLog> {
    public static final String DEFAULT_FLOW_RESULTS_TOPIC = "flows";
    private final SinkMessageKafkaPublisher<FlowDocumentLog, TenantLocationSpecificFlowDocumentLog> kafkaPublisher;
    private final Counter flowCounter;
    private final Counter flowLogCounter;

    private final Counter flowSizeCounter;

    @Autowired
    public FlowKafkaForwarder(SinkMessageKafkaPublisherFactory messagePublisherFactory, TenantLocationSpecificFlowDocumentLogMapper mapper,
                              @Value("${flow.results.kafka-topic:" + DEFAULT_FLOW_RESULTS_TOPIC + "}") String kafkaTopic,
                              MeterRegistry registry) {
        this.kafkaPublisher = messagePublisherFactory.create(
            mapper::mapBareToTenanted,
            kafkaTopic
        );
        Objects.requireNonNull(registry);
        flowCounter = registry.counter(FlowKafkaForwarder.class.getName(), MeteringServerInterceptor.SERVICE_TAG_NAME, "FlowDocument");
        flowLogCounter = registry.counter(FlowKafkaForwarder.class.getName(), MeteringServerInterceptor.SERVICE_TAG_NAME, "FlowDocumentLog");
        flowSizeCounter = registry.counter(FlowKafkaForwarder.class.getName(), MeteringServerInterceptor.SERVICE_TAG_NAME, "FlowDocumentSize");
    }

    @Override
    public SinkModule<FlowDocumentLog, FlowDocumentLog> getModule() {
        return new org.opennms.miniongateway.grpc.server.flows.FlowSinkModule();
    }

    @Override
    public void handleMessage(FlowDocumentLog messageLog) {
        flowCounter.increment(messageLog.getMessageCount());
        // getSerializedSize is heavy duty should remove before production
        flowSizeCounter.increment(messageLog.getSerializedSize());
        flowLogCounter.increment();
        this.kafkaPublisher.send(messageLog);
    }
}
