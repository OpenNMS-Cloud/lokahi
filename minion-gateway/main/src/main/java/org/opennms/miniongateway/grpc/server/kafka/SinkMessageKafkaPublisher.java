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
package org.opennms.miniongateway.grpc.server.kafka;

import com.google.protobuf.Message;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

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

    public SinkMessageKafkaPublisher(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            TenantIDGrpcServerInterceptor tenantInterceptor,
            LocationServerInterceptor locationInterceptor,
            SinkMessageMapper<I, O> mapper,
            String topic,
            MeterRegistry meterRegistry) {
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
        logger.trace(
                "Received {}; sending a {} to kafka topic {}; tenantId: {}; locationId={}; message={}",
                message.getDescriptorForType().getName(),
                mapped.getDescriptorForType().getName(),
                topic,
                tenantId,
                locationId,
                mapped);

        this.meterRegistry
                .timer("kafka.send", "topic", this.topic, "tenant", tenantId, "location", locationId)
                .record(() -> {
                    kafkaTemplate.send(new ProducerRecord<>(topic, mapped.toByteArray()));
                });
    }
}
