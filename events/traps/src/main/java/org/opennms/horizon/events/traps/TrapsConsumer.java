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

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.config.DefaultEventConfDao;
import org.opennms.horizon.config.conf.xml.LogDestType;
import org.opennms.horizon.config.conf.xml.Logmsg;
import org.opennms.horizon.config.xml.Event;
import org.opennms.horizon.config.xml.Events;
import org.opennms.horizon.config.xml.Log;
import org.opennms.horizon.config.xml.Snmp;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.traps.TrapdInstrumentation;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;

@Component
@PropertySource("classpath:application.yml")
public class TrapsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TrapsConsumer.class);

    public static final TrapdInstrumentation trapdInstrumentation = new TrapdInstrumentation();

    @Autowired
    private DefaultEventConfDao eventConfDao;

    @Autowired
    private SnmpHelper snmpHelper;

    @Autowired
    private EventForwarder eventForwarder;

    private EventCreator eventCreator;


    @PostConstruct
    public void init() {
        eventCreator = new EventCreator(eventConfDao, snmpHelper);
    }


    @KafkaListener(topics = "${kafka.traps-topic}", concurrency = "1")
    public void consume(@Payload byte[] data, @Headers Map<String, Object> headers) {

        try {
            TrapLogDTO trapLogDTO = TrapLogDTO.parseFrom(data);
            LOG.debug("Received trap {}", trapLogDTO);
            // Convert to Event
            final Log eventLog = toLog(trapLogDTO);
            // Derive tenant Id
            Optional<String> tenantOptional = getTenantId(headers);
            if (tenantOptional.isEmpty()) {
                // Traps without tenantId are dropped.
                return;
            }
            String tenantId = tenantOptional.get();
            // Convert to events into protobuf format
            EventLog eventLogProto = convertToProtoEvents(eventLog);
            // Send them to kafka
            eventForwarder.sendEvents(eventLogProto, tenantId);

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing traps ", e);
        }
    }

    private EventLog convertToProtoEvents(Log eventLog) {
        EventLog.Builder builder = EventLog.newBuilder();
        eventLog.getEvents().getEventCollection().forEach((event -> {
            builder.addEvent(mapToEventProto(event));
        }));
        return builder.build();
    }

    private org.opennms.horizon.events.proto.Event mapToEventProto(Event event) {
        org.opennms.horizon.events.proto.Event.Builder eventBuilder = org.opennms.horizon.events.proto.Event.newBuilder()
            .setUei(event.getUei())
            .setProducedTime(event.getCreationTime().getTime())
            .setNodeId(event.getNodeid())
            .setLocation(event.getDistPoller())
            .setIpAddress(event.getInterface());
        if (event.getSnmp() != null) {
            eventBuilder.setEventInfo(mapEventInfo(event.getSnmp()));
        }
        // TODO: Add Event Params
        return eventBuilder.build();
    }

    private EventInfo mapEventInfo(Snmp snmp) {
        return EventInfo.newBuilder().setSnmp(SnmpInfo.newBuilder()
            .setId(snmp.getId())
            .setVersion(snmp.getVersion())
            .setGeneric(snmp.getGeneric())
            .setCommunity(snmp.getCommunity())
            .setSpecific(snmp.getSpecific())
            .setTrapOid(snmp.getTrapOID()).build()).build();
    }

    private Optional<String> getTenantId(Map<String, Object> headers) {
        Object tenantId = headers.get("tenant-id");
        // TODO: remove this once tenant is coming from minion gateway
        if (tenantId == null) {
            return Optional.of("opennms-prime");
        }
        if (tenantId instanceof String) {
            return Optional.of((String) tenantId);
        }
        return Optional.empty();
    }


    private Log toLog(TrapLogDTO messageLog) {
        final Log log = new Log();
        final Events events = new Events();
        log.setEvents(events);

        for (TrapDTO eachMessage : messageLog.getTrapDTOList()) {
            try {
                final Event event = eventCreator.createEventFrom(
                    eachMessage,
                    messageLog.getIdentity().getSystemId(),
                    messageLog.getIdentity().getLocation(),
                    InetAddressUtils.getInetAddress(messageLog.getTrapAddress()));
                if (!shouldDiscard(event)) {
                    if (event.getSnmp() != null) {
                        trapdInstrumentation.incTrapsReceivedCount(event.getSnmp().getVersion());
                    }
                    events.addEvent(event);
                } else {
                    LOG.debug("Trap discarded due to matching event having logmsg dest == discardtraps");
                    trapdInstrumentation.incDiscardCount();
                }
            } catch (Throwable e) {
                LOG.error("Unexpected error processing trap: {}", eachMessage, e);
                trapdInstrumentation.incErrorCount();
            }
        }
        return log;
    }

    private boolean shouldDiscard(Event event) {
        org.opennms.horizon.config.conf.xml.Event econf = eventConfDao.findByEvent(event);
        if (econf != null) {
            final Logmsg logmsg = econf.getLogmsg();
            return logmsg != null && LogDestType.DISCARDTRAPS.equals(logmsg.getDest());
        }
        return false;
    }
}
