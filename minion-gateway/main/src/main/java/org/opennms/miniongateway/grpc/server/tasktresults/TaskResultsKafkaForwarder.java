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

package org.opennms.miniongateway.grpc.server.tasktresults;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.protobuf.mapper.TenantedTaskSetResultsMapper;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.taskset.contract.TenantedTaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Forwarder of TaskResults - received via GRPC and forwarded to Kafka.
 */
@Component
public class TaskResultsKafkaForwarder implements MessageConsumer<TaskSetResults, TaskSetResults> {

    public static final String DEFAULT_TASK_RESULTS_TOPIC = "task-set.results";

    private final Logger logger = LoggerFactory.getLogger(TaskResultsKafkaForwarder.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor;
    private final TenantedTaskSetResultsMapper tenantedTaskSetResultsMapper;

    private final String kafkaTopic;

    public TaskResultsKafkaForwarder(
        KafkaTemplate<String, byte[]> kafkaTemplate,
        TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor,
        TenantedTaskSetResultsMapper tenantedTaskSetResultsMapper,
        @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
        String kafkaTopic) {

        this.kafkaTemplate = kafkaTemplate;
        this.tenantIDGrpcInterceptor = tenantIDGrpcInterceptor;
        this.tenantedTaskSetResultsMapper = tenantedTaskSetResultsMapper;
        this.kafkaTopic = kafkaTopic;
    }

    @Override
    public SinkModule<TaskSetResults, TaskSetResults> getModule() {
        return new TaskResultsModule();
    }

    @Override
    public void handleMessage(TaskSetResults messageLog) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcInterceptor.readCurrentContextTenantId();
        logger.debug("Received results; sending to Kafka: tenant-id: {}; kafka-topic={}; message={}", tenantId, kafkaTopic, messageLog);

        // Map to tenanted
        TenantedTaskSetResults tenantedTaskSetResults = tenantedTaskSetResultsMapper.mapBareToTenanted(tenantId, messageLog);

        // Convert to bytes
        byte[] rawContent = tenantedTaskSetResults.toByteArray();
        var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, rawContent);

        // Send to Kafka
        this.kafkaTemplate.send(producerRecord);
    }
}
