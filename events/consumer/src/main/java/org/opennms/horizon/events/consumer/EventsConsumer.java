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

package org.opennms.horizon.events.consumer;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.events.consumer.metrics.TenantMetricsTracker;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@PropertySource("classpath:application.yml")
public class EventsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(EventsConsumer.class);

    private final EventRepository eventRepository;

    private final TenantMetricsTracker metricsTracker;
    @Autowired
    public EventsConsumer(EventRepository eventRepository, TenantMetricsTracker metricsTracker) {
        this.eventRepository = eventRepository;
        this.metricsTracker = metricsTracker;
    }

    @KafkaListener(topics = "${kafka.trap-events-topic}", concurrency = "1")
    public void consume(@Payload byte[] data) {

        try {
            EventLog eventLog = EventLog.parseFrom(data);
            LOG.trace("Received events from kafka {}", eventLog);
            if (Strings.isNullOrEmpty(eventLog.getTenantId())) {
                LOG.warn("TenantId is empty. Dropping events: {}", eventLog);
                return;
            }
            List<Event> eventList = mapEventsFromLog(eventLog);
            eventRepository.saveAll(eventList);
            metricsTracker.addTenantEventSampleCount(eventLog.getTenantId(), eventList.size());
            LOG.info("Persisted {} event(s) in database for tenant {}.", eventList.size(), eventLog.getTenantId());
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Exception while parsing events from payload. Events will be dropped. Payload: {}",
                Arrays.toString(data), e);
        }
    }

    List<Event> mapEventsFromLog(EventLog eventLog) {
        return eventLog.getEventsList().stream()
            .map(this::mapEventFromProto)
            .collect(Collectors.toList());
    }

    private Event mapEventFromProto(org.opennms.horizon.events.proto.Event eventProto) {
        var event = new Event();
        event.setTenantId(eventProto.getTenantId());
        event.setEventUei(eventProto.getUei());
        try {
            event.setIpAddress(InetAddressUtils.getInetAddress(eventProto.getIpAddress()));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Failed to parse IP address: {} for event: {}. Field will not be set.",
                eventProto.getIpAddress(), eventProto);
        }
        event.setNodeId(eventProto.getNodeId());
        event.setProducedTime(LocalDateTime.now());
        var eventParameters = new EventParameters();
        var paramsList = eventProto.getParametersList().stream().map(this::mapEventParam).collect(Collectors.toList());
        eventParameters.setParameters(paramsList);
        event.setEventParameters(eventParameters);
        event.setEventInfo(eventProto.getInfo().toByteArray());
        return event;
    }

    private EventParameter mapEventParam(org.opennms.horizon.events.proto.EventParameter eventParameter) {
        var param = new EventParameter();
        param.setEncoding(eventParameter.getEncoding());
        param.setName(eventParameter.getName());
        param.setType(eventParameter.getType());
        param.setValue(eventParameter.getValue());
        return param;
    }
}
