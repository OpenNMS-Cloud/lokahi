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

import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.protobuf.mapper.TenantLocationSpecificTaskSetResultsMapper;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Forwarder of TaskResults - received via GRPC and forwarded to Kafka.
 */
@Component
public class TaskResultsKafkaForwarder implements MessageConsumer<TaskSetResults, TaskSetResults> {

    public static final String DEFAULT_TASK_RESULTS_TOPIC = "task-set.results";
    private final SinkMessageKafkaPublisher<TaskSetResults, TenantLocationSpecificTaskSetResults> kafkaPublisher;

    @Autowired
    public TaskResultsKafkaForwarder(SinkMessageKafkaPublisherFactory messagePublisherFactory, TenantLocationSpecificTaskSetResultsMapper mapper,
        @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}") String kafkaTopic) {
        this.kafkaPublisher = messagePublisherFactory.create(
            mapper::mapBareToTenanted,
            kafkaTopic
        );
    }

    @Override
    public SinkModule<TaskSetResults, TaskSetResults> getModule() {
        return new TaskResultsModule();
    }

    @Override
    public void handleMessage(TaskSetResults message) {
        this.kafkaPublisher.send(message);
    }
}
