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
package org.opennms.horizon.events.traps;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventForwarder {

    private static final Logger LOG = LoggerFactory.getLogger(EventForwarder.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private final String trapEventsTopic;
    private final String internalEventsTopic;

    public EventForwarder(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            @Value("${kafka.trap-events-topic}") String trapEventsTopic,
            @Value("${kafka.internal-events-topic}") String internalEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.trapEventsTopic = trapEventsTopic;
        this.internalEventsTopic = internalEventsTopic;
    }

    public void sendTrapEvents(EventLog eventLog) {
        LOG.info("Sending {} events to events topic for tenant: {}", eventLog.getEventsCount(), eventLog.getTenantId());
        var producerRecord = new ProducerRecord<String, byte[]>(trapEventsTopic, eventLog.toByteArray());
        kafkaTemplate.send(producerRecord);
    }

    public void sendInternalEvent(Event event) {
        LOG.info(
                "Sending event with UEI: {} for interface: {} for tenantId={}; locationId={}",
                event.getUei(),
                event.getIpAddress(),
                event.getTenantId(),
                event.getLocationId());
        var eventLog = EventLog.newBuilder().addEvents(event).build();
        var producerRecord = new ProducerRecord<String, byte[]>(internalEventsTopic, eventLog.toByteArray());
        kafkaTemplate.send(producerRecord);
    }
}
