/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
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

package org.opennms.miniongateway.grpc.server.heartbeat;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.mapper.TenantLocationSpecificHeartbeatMessageMapper;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import io.opentelemetry.api.trace.Span;

/**
 * Forwarder of Heartbeat messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class HeartbeatKafkaForwarder implements MessageConsumer<HeartbeatMessage, HeartbeatMessage> {
    public static final String DEFAULT_TASK_RESULTS_TOPIC = "heartbeat";

    private final Logger logger = LoggerFactory.getLogger(HeartbeatKafkaForwarder.class);

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    @Autowired
    private TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor;

    @Autowired
    private LocationServerInterceptor locationServerInterceptor;

    @Autowired
    private TenantLocationSpecificHeartbeatMessageMapper tenantLocationSpecificHeartbeatMessageMapper;

    @Override
    public SinkModule<HeartbeatMessage, HeartbeatMessage> getModule() {
        return new HeartbeatModule();
    }

    @Override
    public void handleMessage(HeartbeatMessage heartbeatMessage) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcInterceptor.readCurrentContextTenantId();
        // And the locationId from its Interceptor
        String locationId = locationServerInterceptor.readCurrentContextLocationId();

        logger.info("Received heartbeat; sending to Kafka: tenantId={}; locationId={}; kafka-topic={}; message={}", tenantId, locationId, kafkaTopic, heartbeatMessage);
        Span.current().setAttribute("message", heartbeatMessage.toString());

        TenantLocationSpecificHeartbeatMessage mapped = tenantLocationSpecificHeartbeatMessageMapper.mapBareToTenanted(tenantId, locationId, heartbeatMessage);

        byte[] rawContent = mapped.toByteArray();
        var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, rawContent);

        this.kafkaTemplate.send(producerRecord);
    }

}
