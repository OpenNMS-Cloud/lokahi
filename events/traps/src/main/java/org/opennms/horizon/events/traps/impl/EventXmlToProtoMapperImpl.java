/*
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
 *
 */

package org.opennms.horizon.events.traps.impl;

import com.google.common.base.Strings;
import org.opennms.horizon.events.api.Severity;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.EventParameter;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.opennms.horizon.events.traps.EventXmlToProtoMapper;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Parm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class EventXmlToProtoMapperImpl implements EventXmlToProtoMapper {

    private static final Logger LOG = LoggerFactory.getLogger(EventXmlToProtoMapperImpl.class);

    public org.opennms.horizon.events.proto.Event convert(Event event, String tenantId) {
        org.opennms.horizon.events.proto.Event.Builder eventBuilder = org.opennms.horizon.events.proto.Event.newBuilder()
            .setTenantId(tenantId)
            .setUei(event.getUei())
            .setProducedTimeMs(event.getCreationTime().getTime())
            .setNodeId(event.getNodeid())
            .setLocationId(event.getDistPoller())
            .setIpAddress(event.getInterface());

        if (event.getDescr() != null) {
            eventBuilder.setDescription(event.getDescr());
        }
        if (event.getLogmsg() != null) {
            eventBuilder.setLogMessage(event.getLogmsg().getContent());
        }

        mapEventInfo(event, eventBuilder);

        List<EventParameter> eventParameters = mapEventParams(event);
        eventBuilder.addAllParameters(eventParameters);
        return eventBuilder.build();
    }

    private void mapEventInfo(Event event, org.opennms.horizon.events.proto.Event.Builder eventBuilder) {
        var snmp = event.getSnmp();
        if (snmp != null) {
            var eventInfo = EventInfo.newBuilder().setSnmp(SnmpInfo.newBuilder()
                .setId(snmp.getId())
                .setVersion(snmp.getVersion())
                .setGeneric(snmp.getGeneric())
                .setCommunity(snmp.getCommunity())
                .setSpecific(snmp.getSpecific())
                .setTrapOid(snmp.getTrapOID()).build()).build();
            eventBuilder.setInfo(eventInfo);
        }
    }

    private List<EventParameter> mapEventParams(Event event) {

        return event.getParmCollection().stream().map(this::mapEventParm)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private EventParameter mapEventParm(Parm parm) {
        if (parm.isValid()) {
            var eventParm = EventParameter.newBuilder()
                .setName(parm.getParmName())
                .setType(parm.getValue().getType())
                .setEncoding(parm.getValue().getEncoding())
                .setValue(parm.getValue().getContent()).build();
            return eventParm;
        }
        return null;
    }
}
