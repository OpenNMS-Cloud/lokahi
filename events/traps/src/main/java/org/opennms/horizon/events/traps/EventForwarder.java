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

    public EventForwarder(KafkaTemplate<String, byte[]> kafkaTemplate,
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
        LOG.info("Sending event with UEI: {} for interface: {} for tenantId={}; locationId={}", event.getUei(),
            event.getIpAddress(), event.getTenantId(), event.getLocationId());
        var eventLog = EventLog.newBuilder().addEvents(event).build();
        var producerRecord = new ProducerRecord<String, byte[]>(internalEventsTopic, eventLog.toByteArray());
        kafkaTemplate.send(producerRecord);
    }
}
