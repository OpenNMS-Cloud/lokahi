/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.events.traps;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Setter;
import org.opennms.horizon.events.EventConstants;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@PropertySource("classpath:application.yml")
public class TrapsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TrapsConsumer.class);

    @Autowired
    @Setter
    private EventForwarder eventForwarder;

    @Autowired
    @Setter
    private EventLogXmlToProtoMapper eventLogXmlToProtoMapper;

    @Autowired
    @Setter
    private TrapLogProtoToEventLogXmlMapper trapLogProtoToXmlMapper;

    @KafkaListener(topics = "${kafka.raw-traps-topic}", concurrency = "1")
    public void consume(@Payload byte[] data) {

        try {
            TenantLocationSpecificTrapLogDTO tenantLocationSpecificTrapLogDTO
                = TenantLocationSpecificTrapLogDTO.parseFrom(data);

            LOG.debug("Received trap {}", tenantLocationSpecificTrapLogDTO);

            String tenantId = tenantLocationSpecificTrapLogDTO.getTenantId();

            // Convert to Event
            // TODO: do we need to use this intermediate format?
            Log eventLog = trapLogProtoToXmlMapper.convert(tenantLocationSpecificTrapLogDTO);

            // Convert to events into protobuf format
            EventLog eventLogProto = eventLogXmlToProtoMapper.convert(eventLog, tenantId);

            // Send them to kafka
            eventForwarder.sendTrapEvents(eventLogProto);

            eventLogProto.getEventsList().stream()
                .filter(event-> ( event.getNodeId() <= 0 ))
                .forEach(event -> sendNewSuspectEvent(event, tenantId));

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing traps", e);
        }
    }

    private void sendNewSuspectEvent(org.opennms.horizon.events.proto.Event event, String tenantId) {
        var newEvent = org.opennms.horizon.events.proto.Event.newBuilder()
            .setTenantId(tenantId)
            .setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
            .setIpAddress(event.getIpAddress())
            .setLocationId(event.getLocationId())
            .setInfo(event.getInfo())
            .addAllParameters(event.getParametersList())
            .build();

        eventForwarder.sendInternalEvent(newEvent);

        LOG.info("Sent new suspect event for interface {}", event.getIpAddress());
    }
}
