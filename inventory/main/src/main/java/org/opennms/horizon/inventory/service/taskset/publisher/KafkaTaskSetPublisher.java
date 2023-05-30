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

package org.opennms.horizon.inventory.service.taskset.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.contract.PublishType;
import org.opennms.taskset.contract.TaskDefPub;
import org.opennms.taskset.contract.TaskDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
@PropertySource("classpath:application.yml")
@Primary
public class KafkaTaskSetPublisher implements TaskSetPublisher {

    private static final String DEFAULT_TASK_SET_PUBLISH_TOPIC = "task-set-publisher";
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.task-set-publisher:" + DEFAULT_TASK_SET_PUBLISH_TOPIC + "}")
    private String kafkaTopic;

    @Override
    public void publishNewTasks(String tenantId, String location, List<TaskDefinition> taskList) {

        log.info("Publishing task updates for location {} with tenantId {}, taskDef = {}", location, tenantId, taskList);
        var tasksetPub = TaskDefPub.newBuilder()
            .setPublishType(PublishType.UPDATE)
            .setLocation(location).setTenantId(tenantId)
            .addAllTaskDef(taskList).build();
        kafkaTemplate.send(kafkaTopic, tasksetPub.toByteArray());
    }

    @Override
    public void publishTaskDeletion(String tenantId, String location, List<TaskDefinition> taskList) {

        log.info("Publishing task removal for location {} with tenantId {}, taskDef = {}", location, tenantId, taskList);
        var tasksetPub = TaskDefPub.newBuilder()
            .setPublishType(PublishType.DELETE)
            .setLocation(location).setTenantId(tenantId)
            .addAllTaskDef(taskList).build();
        kafkaTemplate.send(kafkaTopic, tasksetPub.toByteArray());
    }
}
