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

import com.google.protobuf.Message;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Forwarder of Heartbeat messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class HeartbeatKafkaForwarder implements MessageConsumer<Message, Message> {
    public static final String DEFAULT_TASK_RESULTS_TOPIC = "heartbeat";

    private final Logger logger = LoggerFactory.getLogger(HeartbeatKafkaForwarder.class);
    private final RateLimitedLog usingDefaultTenantIdLog =
        RateLimitedLog
            .withRateLimit(logger)
            .maxRate(1)
            .every(Duration.ofMinutes(1))
            .build();

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    @Autowired
    private TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor;

    @Override
    public SinkModule<Message, Message> getModule() {
        return new HeartbeatModule();
    }

    @Override
    public void handleMessage(Message messageLog) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcInterceptor.readCurrentContextTenantId();
        logger.info("Received heartbeat; sending to Kafka: tenant-id: {}; kafka-topic={}; message={}", tenantId, kafkaTopic, messageLog);
        byte[] rawContent = messageLog.toByteArray();

        var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, rawContent);
        // add tenant id to kafka header
        producerRecord.headers().add(new RecordHeader(GrpcConstants.TENANT_ID_KEY,
            tenantId.getBytes(StandardCharsets.UTF_8)));

        this.kafkaTemplate.send(producerRecord);
    }

}
