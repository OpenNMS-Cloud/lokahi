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

package org.opennms.miniongateway.taskset.service;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Setter;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TaskSetPublishKafkaConsumer {

    public static final String DEFAULT_TASKSET_PUBLISH_TOPIC = "task-set-publisher";
    private static final Logger LOG = LoggerFactory.getLogger(TaskSetPublishKafkaConsumer.class);

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${task.results.kafka-topic:" + DEFAULT_TASKSET_PUBLISH_TOPIC + "}")
    private String kafkaTopic;

    @Autowired
    @Setter // Testability
    private TaskSetStorage taskSetStorage;

    @Autowired
    @Setter // Testability
    private TaskSetGrpcServiceUpdateProcessorFactory taskSetGrpcServiceUpdateProcessorFactory;


    @KafkaListener(topics = "${task.results.kafka-topic:" + DEFAULT_TASKSET_PUBLISH_TOPIC + "}", concurrency = "1")
    public void receiveMessage(@Payload byte[] data) {

        try {
            UpdateTasksRequest request = UpdateTasksRequest.parseFrom(data);
            LOG.info("Received taskset update {}", request);
            TaskSetGrpcServiceUpdateProcessor updateProcessor = taskSetGrpcServiceUpdateProcessorFactory.create(request);

            try {
                taskSetStorage.atomicUpdateTaskSetForLocation(request.getTenantId(), request.getLocationId(), updateProcessor);
            } catch (RuntimeException rtExc) {
                // Log exceptions here that might otherwise get swallowed
                LOG.warn("error applying task set updates", rtExc);
                throw rtExc;
            }
        } catch (InvalidProtocolBufferException e) {

        }
    }
}
