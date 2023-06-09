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

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentLogMapper;
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

/**
 * Forwarder of Flow messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class FlowKafkaForwarder implements MessageConsumer<FlowDocumentLog, FlowDocumentLog> {
    public static final String DEFAULT_TASK_RESULTS_TOPIC = "flows";

    private final Logger logger = LoggerFactory.getLogger(FlowKafkaForwarder.class);

    private final TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor;

    private final LocationServerInterceptor locationServerInterceptor;

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private final TenantLocationSpecificFlowDocumentLogMapper tenantLocationSpecificFlowDocumentLogMapper;

    @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    public FlowKafkaForwarder(@Autowired TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor,
                              @Autowired LocationServerInterceptor locationServerInterceptor,
                              @Autowired KafkaTemplate<String, byte[]> kafkaTemplate,
                              @Autowired TenantLocationSpecificFlowDocumentLogMapper tenantLocationSpecificFlowDocumentLogMapper) {
        this.tenantIDGrpcInterceptor = tenantIDGrpcInterceptor;
        this.locationServerInterceptor = locationServerInterceptor;
        this.kafkaTemplate = kafkaTemplate;
        this.tenantLocationSpecificFlowDocumentLogMapper = tenantLocationSpecificFlowDocumentLogMapper;
    }

    @Override
    public SinkModule<FlowDocumentLog, FlowDocumentLog> getModule() {
        return new org.opennms.miniongateway.grpc.server.flows.FlowSinkModule();
    }

    @Override
    public void handleMessage(FlowDocumentLog messageLog) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcInterceptor.readCurrentContextTenantId();
        // Ditto for location
        String location = locationServerInterceptor.readCurrentContextLocation();
        logger.trace("Received flow; sending to Kafka: tenant-id: {}; kafka-topic={}; message={}", tenantId, kafkaTopic, messageLog);


        var tenantLocationSpecificFlowDocumentLog =
            tenantLocationSpecificFlowDocumentLogMapper.mapBareToTenanted(tenantId, location, messageLog);

        sendToKafka(tenantLocationSpecificFlowDocumentLog);
    }

//========================================
// INTERNALS
//----------------------------------------

    private void sendToKafka(TenantLocationSpecificFlowDocumentLog tenantLocationSpecificFlowDocumentLog) {
        byte[] rawContent = tenantLocationSpecificFlowDocumentLog.toByteArray();
        var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, rawContent);

        this.kafkaTemplate.send(producerRecord);
    }
}
