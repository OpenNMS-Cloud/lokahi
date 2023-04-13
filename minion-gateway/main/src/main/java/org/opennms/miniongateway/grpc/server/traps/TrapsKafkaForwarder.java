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

package org.opennms.miniongateway.grpc.server.traps;

import com.google.protobuf.Message;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.grpc.traps.contract.mapper.TenantLocationSpecificTrapLogDTOMapper;
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

/**
 * Forwarder of Trap messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class TrapsKafkaForwarder implements MessageConsumer<TrapLogDTO, TrapLogDTO> {

    public static final String DEFAULT_TASK_RESULTS_TOPIC = "traps";

    private final Logger logger = LoggerFactory.getLogger(TrapsKafkaForwarder.class);

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Autowired
    private TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor;

    @Autowired
    private LocationServerInterceptor locationServerInterceptor;

    @Autowired
    private TenantLocationSpecificTrapLogDTOMapper tenantLocationSpecificTrapLogDTOMapper;

    @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    @Override
    public SinkModule<TrapLogDTO, TrapLogDTO> getModule() {
        return new TrapSinkModule();
    }

    @Override
    public void handleMessage(TrapLogDTO messageLog) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcInterceptor.readCurrentContextTenantId();
        // Ditto for location
        String location = locationServerInterceptor.readCurrentContextLocation();

        // String location
        logger.info("Received traps; sending to Kafka: tenant-id={}; kafka-topic={}; message={}", tenantId, kafkaTopic, messageLog);

        TenantLocationSpecificTrapLogDTO tenantLocationSpecificTrapLogDTO =
            tenantLocationSpecificTrapLogDTOMapper.mapBareToTenanted(tenantId, location, messageLog);

        sendToKafka(tenantLocationSpecificTrapLogDTO);
    }

//========================================
// Internals
//----------------------------------------

    private void sendToKafka(TenantLocationSpecificTrapLogDTO tenantLocationSpecificTrapLogDTO) {
        byte[] rawContent = tenantLocationSpecificTrapLogDTO.toByteArray();
        var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, rawContent);

        this.kafkaTemplate.send(producerRecord);
    }
}
