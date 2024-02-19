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
package org.opennms.horizon.inventory.component;

import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.events.proto.EventLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class InternalEventProducer {

    private static final String DEFAULT_TOPIC = "internal-event";

    @Value("${kafka.topics.internal-events:" + DEFAULT_TOPIC + "}")
    private String eventTopic;

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public InternalEventProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(EventLog eventLog) {
        log.info("Sending internal events {}", eventLog);
        kafkaTemplate.send(eventTopic, eventLog.toByteArray());
    }
}
