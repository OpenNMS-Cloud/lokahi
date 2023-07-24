/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.miniongateway.grpc.server.kafka;

import com.google.protobuf.Message;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * A helper class which produces kafka messages.
 *
 * It additionally retrieves tenant information from present context.
 * @param <I> Input message (grpc side) kind
 * @param <O> Output message (kafka side) type
 */
public class SinkMessageKafkaPublisher<I extends Message, O extends Message> {

    private final Logger logger = LoggerFactory.getLogger(SinkMessageKafkaPublisher.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final TenantIDGrpcServerInterceptor tenantInterceptor;
    private final LocationServerInterceptor locationInterceptor;
    private final SinkMessageMapper<I, O> mapper;
    private final String topic;
    private final MeterRegistry meterRegistry;

    public SinkMessageKafkaPublisher(KafkaTemplate<String, byte[]> kafkaTemplate,
                                     TenantIDGrpcServerInterceptor tenantInterceptor, LocationServerInterceptor locationInterceptor,
                                     SinkMessageMapper<I, O> mapper, String topic, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.tenantInterceptor = tenantInterceptor;
        this.locationInterceptor = locationInterceptor;
        this.mapper = mapper;
        this.topic = topic;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Map passed In message to a backend message which is then used as a payload for record sent to Kafka.
     *
     * @param message content to include as the message payload.
     */
    public void send(I message) {
        String tenantId = tenantInterceptor.readCurrentContextTenantId();
        String locationId = locationInterceptor.readCurrentContextLocationId();

        O mapped = mapper.map(tenantId, locationId, message);
        logger.trace("Received {}; sending a {} to kafka topic {}; tenantId: {}; locationId={}; message={}",
            message.getDescriptorForType().getName(), mapped.getDescriptorForType().getName(), topic, tenantId, locationId, mapped);

        this.meterRegistry.timer("kafka.send",
                "topic", this.topic,
                "tenant", tenantId,
                "location", locationId)
            .record(() -> {
                kafkaTemplate.send(new ProducerRecord<>(topic, mapped.toByteArray()));
            });
    }
}
