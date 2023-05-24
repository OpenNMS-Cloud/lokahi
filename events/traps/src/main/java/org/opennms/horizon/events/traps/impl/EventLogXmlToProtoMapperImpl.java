package org.opennms.horizon.events.traps.impl;

import lombok.Setter;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.traps.EventLogXmlToProtoMapper;
import org.opennms.horizon.events.traps.EventXmlToProtoMapper;
import org.opennms.horizon.events.xml.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventLogXmlToProtoMapperImpl implements EventLogXmlToProtoMapper {

    @Autowired
    @Setter
    private EventXmlToProtoMapper eventXmlToProtoMapper;

    @Override
    public EventLog convert(Log eventLog, String tenantId) {
        EventLog.Builder builder = EventLog.newBuilder()
                .setTenantId(tenantId);

        eventLog.getEvents().getEventCollection().forEach(
            (event ->
                builder.addEvents(
                    eventXmlToProtoMapper.convert(event, tenantId)
                )
            )
        );

        return builder.build();
    }
}
