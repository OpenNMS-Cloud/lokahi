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

package org.opennms.horizon.events.consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@PropertySource("classpath:application.yml")
public class EventsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(EventsConsumer.class);

    private final EventRepository eventRepository;

    @Autowired
    public EventsConsumer(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @KafkaListener(topics = "${kafka.events-topic}", concurrency = "1")
    public void consume(@Payload byte[] data, @Headers Map<String, Object> headers) {

        try {
            EventLog eventLog = EventLog.parseFrom(data);
            LOG.info("Received events from kafka {}", eventLog);
            var tenantOptional = getTenantId(headers);
            if (tenantOptional.isEmpty()) {
                LOG.warn("TenantId is empty, dropping events {}", eventLog);
                return;
            }
            String tenantId = tenantOptional.get();
            List<Event> eventList = mapEventsFromLog(eventLog, tenantId);
            eventRepository.saveAll(eventList);
            LOG.info("Persisted {} events in DB", eventList.size());
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Exception while parsing events from payload", e);
        }

    }

    List<Event> mapEventsFromLog(EventLog eventLog, String tenantId) {
        return eventLog.getEventList().stream().map(eventProto -> {
            try {
                return mapEventFromProto(eventProto, tenantId);
            } catch (UnknownHostException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Event mapEventFromProto(org.opennms.horizon.events.proto.Event eventProto, String tenantId) throws UnknownHostException {
        var event = new Event();
        event.setTenantId(tenantId);
        event.setEventUei(eventProto.getUei());
        event.setIpAddress(InetAddressUtils.getInetAddress(eventProto.getIpAddress()));
        event.setNodeId(event.getNodeId());
        event.setProducedTime(LocalDateTime.now());
        var eventParameters = new EventParameters();
        var paramsList = eventProto.getEventParamsList().stream().map(this::mapEventParam).collect(Collectors.toList());
        eventParameters.setParameters(paramsList);
        event.setEventParameters(eventParameters);
        event.setEventInfo(eventProto.getEventInfo().toByteArray());
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


    private Optional<String> getTenantId(Map<String, Object> headers) {
        Object tenantId = headers.get(GrpcConstants.TENANT_ID_KEY);
        if (tenantId instanceof byte[]) {
            return Optional.of(new String((byte[]) tenantId));
        }
        return Optional.empty();
    }
}
