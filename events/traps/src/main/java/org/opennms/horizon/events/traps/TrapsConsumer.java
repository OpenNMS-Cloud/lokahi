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

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;

import org.opennms.horizon.events.EventConstants;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.events.grpc.client.InventoryClient;
import org.opennms.horizon.events.proto.AlarmData;
import org.opennms.horizon.events.proto.EventSeverity;
import org.opennms.horizon.events.proto.ManagedObject;
import org.opennms.horizon.events.proto.UpdateField;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.events.xml.Parm;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventParameter;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.constants.GrpcConstants;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
@PropertySource("classpath:application.yml")
public class TrapsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TrapsConsumer.class);

    private final EventConfDao eventConfDao;

    private final SnmpHelper snmpHelper;

    private final TrapEventForwarder eventForwarder;
    private final InventoryClient inventoryClient;

    @Autowired
    public TrapsConsumer(EventConfDao eventConfDao, SnmpHelper snmpHelper,
                         TrapEventForwarder eventForwarder, InventoryClient inventoryClient) {
        this.eventConfDao = eventConfDao;
        this.snmpHelper = snmpHelper;
        this.eventForwarder = eventForwarder;
        this.inventoryClient = inventoryClient;
    }

    private EventFactory eventFactory;


    @PostConstruct
    public void init() {
        eventFactory = new EventFactory(eventConfDao, snmpHelper, inventoryClient);
    }


    @KafkaListener(topics = "${kafka.traps-topic}", concurrency = "1")
    public void consume(@Payload byte[] data, @Headers Map<String, Object> headers) {

        try {
            TrapLogDTO trapLogDTO = TrapLogDTO.parseFrom(data);
            LOG.debug("Received trap {}", trapLogDTO);
            // Derive tenant Id
            Optional<String> tenantOptional = getTenantId(headers);
            if (tenantOptional.isEmpty()) {
                // Traps without tenantId are dropped.
                LOG.warn("Received {} traps without tenantId, dropping", trapLogDTO.getTrapDTOList().size());
                return;
            }
            String tenantId = tenantOptional.get();

            // Convert to Event
            final Log eventLog = toLog(trapLogDTO, tenantId);

            // Convert to events into protobuf format
            EventLog eventLogProto = convertToProtoEvents(eventLog);
            // Send them to kafka
            eventForwarder.sendEvents(eventLogProto, tenantId);

            eventLogProto.getEventList().stream()
                .filter(e-> e.getNodeId() <= 0)
                .forEach(e ->{
                    sendNewSuspectEvent(e, tenantId);
                    LOG.info("Sent new suspect event for interface {}", e.getIpAddress());
                });

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing traps ", e);
        }
    }

    private void sendNewSuspectEvent(org.opennms.horizon.events.proto.Event event, String tenantId) {
        var newEvent = org.opennms.horizon.events.proto.Event.newBuilder()
            .setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
            .setIpAddress(event.getIpAddress())
            .setLocation(event.getLocation())
            .setEventInfo(event.getEventInfo())
            .addAllEventParams(event.getEventParamsList())
            .build();
        eventForwarder.sendInternalEvents(newEvent, tenantId);
    }

    private EventLog convertToProtoEvents(Log eventLog) {
        EventLog.Builder builder = EventLog.newBuilder();
        eventLog.getEvents().getEventCollection().forEach((event -> builder.addEvent(mapToEventProto(event))));
        return builder.build();
    }

    private org.opennms.horizon.events.proto.Event mapToEventProto(Event event) {
        org.opennms.horizon.events.proto.Event.Builder eventBuilder = org.opennms.horizon.events.proto.Event.newBuilder()
            .setUei(event.getUei())
            .setProducedTime(event.getCreationTime().getTime())
            .setNodeId(event.getNodeid())
            .setLocation(event.getDistPoller())
            .setIpAddress(event.getInterface());

        mapSeverity(event, eventBuilder);
        mapAlarmData(event, eventBuilder);
        mapEventInfo(event, eventBuilder);

        List<EventParameter> eventParameters = mapEventParams(event);
        eventBuilder.addAllEventParams(eventParameters);
        return eventBuilder.build();
    }

    static void mapSeverity(Event event, org.opennms.horizon.events.proto.Event.Builder eventBuilder) {
        if (!Strings.isNullOrEmpty(event.getSeverity())) {
            String severity = event.getSeverity().toUpperCase(Locale.ROOT);
            try {
                EventSeverity eventSeverity = EventSeverity.valueOf(severity);
                eventBuilder.setEventSeverity(eventSeverity);
            } catch (IllegalArgumentException iae) {
                LOG.warn("No matching event severity for {} in proto", severity);
            }
        }
    }

    static void mapEventInfo(Event event, org.opennms.horizon.events.proto.Event.Builder eventBuilder) {
        var snmp = event.getSnmp();
        if (snmp != null) {
            var eventInfo = EventInfo.newBuilder().setSnmp(SnmpInfo.newBuilder()
                .setId(snmp.getId())
                .setVersion(snmp.getVersion())
                .setGeneric(snmp.getGeneric())
                .setCommunity(snmp.getCommunity())
                .setSpecific(snmp.getSpecific())
                .setTrapOid(snmp.getTrapOID()).build()).build();
            eventBuilder.setEventInfo(eventInfo);
        }
    }

    private List<EventParameter> mapEventParams(Event event) {

        return event.getParmCollection().stream().map(this::mapEventParm)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private Optional<EventParameter> mapEventParm(Parm parm) {
        if (parm.isValid()) {
            var eventParm = EventParameter.newBuilder()
                .setName(parm.getParmName())
                .setType(parm.getValue().getType())
                .setEncoding(parm.getValue().getEncoding())
                .setValue(parm.getValue().getContent()).build();
            return Optional.of(eventParm);
        }
        return Optional.empty();
    }

    static void mapAlarmData(Event event, org.opennms.horizon.events.proto.Event.Builder eventBuilder) {
        var alarmData = event.getAlarmData();
        if (alarmData != null) {
            AlarmData.Builder builder = AlarmData.newBuilder();
            builder.setReductionKey(alarmData.getReductionKey());
            builder.setAlarmType(alarmData.getAlarmType());
            builder.setClearKey(Optional.ofNullable(alarmData.getClearKey()).orElse(AlarmData.getDefaultInstance().getClearKey()));
            builder.setAutoClean(Optional.ofNullable(alarmData.getAutoClean()).orElse(AlarmData.getDefaultInstance().getAutoClean()));
            if (alarmData.getManagedObject() != null) {
                builder.setManagedObject(ManagedObject.newBuilder()
                    .setType(alarmData.getManagedObject().getType()).build());
            }
            alarmData.getUpdateFieldList().forEach(updateField ->
                builder.addUpdateField(UpdateField.newBuilder()
                    .setFieldName(updateField.getFieldName())
                    .setUpdateOnReduction(updateField.isUpdateOnReduction()).build()));
            eventBuilder.setAlarmData(builder.build());
        }
    }

    private Optional<String> getTenantId(Map<String, Object> headers) {
        Object tenantId = headers.get(GrpcConstants.TENANT_ID_KEY);
        if (tenantId instanceof byte[]) {
            return Optional.of(new String((byte[]) tenantId));
        }
        return Optional.empty();
    }


    private Log toLog(TrapLogDTO messageLog, String tenantId) {
        final Log log = new Log();
        final Events events = new Events();
        log.setEvents(events);

        // TODO: Add metrics for Traps received/error/dropped.
        for (TrapDTO eachMessage : messageLog.getTrapDTOList()) {
            try {
                final Event event = eventFactory.createEventFrom(
                    eachMessage,
                    messageLog.getIdentity().getSystemId(),
                    messageLog.getIdentity().getLocation(),
                    InetAddressUtils.getInetAddress(messageLog.getTrapAddress()),
                    tenantId);
                if (event != null) {
                    events.addEvent(event);
                }
            } catch (Throwable e) {
                LOG.error("Unexpected error processing trap: {}", eachMessage, e);
            }
        }
        return log;
    }

}
