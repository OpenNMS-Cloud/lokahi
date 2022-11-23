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

package org.opennms.horizon.inventory.component;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.service.taskset.response.DetectorResponseService;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSetResultsConsumer {
    private final DetectorResponseService detectorResponseService;

    @KafkaListener(topics = "${kafka.topics.task-set-results}", concurrency = "1")
    public void receiveMessage(byte[] data) {
        try {
            TaskSetResults message = TaskSetResults.parseFrom(data);

            for (TaskResult taskResult : message.getResultsList()) {

                String location = taskResult.getLocation();

                if (taskResult.hasDetectorResponse()) {
                    detectorResponseService.accept(location, taskResult.getDetectorResponse());
                }
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing task set results", e);
        }
    }
}
