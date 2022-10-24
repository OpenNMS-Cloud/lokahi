/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.horizon.db.model.mapper;

import java.net.InetAddress;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.opennms.horizon.shared.utils.IPAddress;
import org.opennms.horizon.db.model.OnmsEvent;
import org.opennms.horizon.db.model.OnmsEventParameter;
import org.opennms.horizon.db.model.OnmsServiceType;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.shared.dto.event.EventDTO;
import org.opennms.horizon.shared.dto.event.EventParameterDTO;
import org.opennms.horizon.shared.dto.event.ServiceTypeDTO;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {})
public abstract class EventMapper {

    @Autowired
    private EventConfDao eventConfDao;

    @Mappings({
            @Mapping(source = "eventUei", target = "uei"),
            @Mapping(source = "eventTime", target = "time"),
            @Mapping(source = "eventHost", target = "host"),
            @Mapping(source = "eventSource", target = "source"),
            @Mapping(source = "ipAddr", target = "ipAddress"),
            @Mapping(source = "eventSnmpHost", target = "snmpHost"),
            @Mapping(source = "eventSnmp", target = "snmp"),
            @Mapping(source = "eventCreateTime", target = "createTime"),
            @Mapping(source = "eventDescr", target = "description"),
            @Mapping(source = "eventLogGroup", target = "logGroup"),
            @Mapping(source = "eventLogMsg", target = "logMessage"),
            @Mapping(source = "eventPathOutage", target = "pathOutage"),
            @Mapping(source = "eventCorrelation", target = "correlation"),
            @Mapping(source = "eventSuppressedCount", target = "suppressedCount"),
            @Mapping(source = "eventOperInstruct", target = "operatorInstructions"),
            @Mapping(source = "eventAutoAction", target = "autoAction"),
            @Mapping(source = "eventOperAction", target = "operatorAction"),
            @Mapping(source = "eventOperActionMenuText", target = "operationActionMenuText"),
            @Mapping(source = "eventNotification", target = "notification"),
            @Mapping(source = "eventTTicket", target = "troubleTicket"),
            @Mapping(source = "eventTTicketState", target = "troubleTicketState"),
            @Mapping(source = "eventMouseOverText", target = "mouseOverText"),
            @Mapping(source = "eventLog", target = "log"),
            @Mapping(source = "eventDisplay", target = "display"),
            @Mapping(source = "eventAckUser", target = "ackUser"),
            @Mapping(source = "eventAckTime", target = "ackTime"),
            @Mapping(source = "distPoller.location", target = "location"),
            @Mapping(source = "severityLabel", target = "severity")
    })
    public abstract EventDTO eventToEventDTO(OnmsEvent event);

    String inetAddressToString(InetAddress inetAddr) {
        return inetAddr == null? null : new IPAddress(inetAddr).toDbString();
    }

    InetAddress stringToInetAddress(String ipAddr) {
        return (ipAddr == null || ipAddr.isEmpty())? null : new IPAddress(ipAddr).toInetAddress();
    }

    @InheritInverseConfiguration
    public abstract OnmsEvent eventDTOToEvent(EventDTO event);

    public abstract ServiceTypeDTO serviceTypeToServiceTypeDTO(OnmsServiceType serviceType);

    public abstract EventParameterDTO eventParameterToEventParameterDTO(OnmsEventParameter eventParameter);

    @AfterMapping
    protected void fillEvent(OnmsEvent event, @MappingTarget EventDTO eventDTO) {
        final List<OnmsEventParameter> eventParms = event.getEventParameters();
        if (eventParms != null) {
            eventDTO.setParameters(eventParms.stream()
                    .map(this::eventParameterToEventParameterDTO)
                    .collect(Collectors.toList()));
        }
        eventDTO.setSeverity(event.getSeverityLabel());
        eventDTO.setLabel(eventConfDao.getEventLabel(eventDTO.getUei()));
    }

    public void setEventConfDao(EventConfDao eventConfDao) {
        this.eventConfDao = eventConfDao;
    }
}
