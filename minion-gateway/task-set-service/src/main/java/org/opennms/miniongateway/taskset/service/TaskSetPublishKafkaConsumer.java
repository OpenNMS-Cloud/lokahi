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
            TaskSetGrpcServiceUpdateProcessor updateProcessor =
                    taskSetGrpcServiceUpdateProcessorFactory.create(request);

            try {
                taskSetStorage.atomicUpdateTaskSetForLocation(
                        request.getTenantId(), request.getLocationId(), updateProcessor);
            } catch (RuntimeException rtExc) {
                // Log exceptions here that might otherwise get swallowed
                LOG.warn("error applying task set updates", rtExc);
                throw rtExc;
            }
        } catch (InvalidProtocolBufferException e) {

        }
    }
}
