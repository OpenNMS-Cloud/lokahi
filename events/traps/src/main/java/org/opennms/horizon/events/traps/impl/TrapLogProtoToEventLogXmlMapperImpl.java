package org.opennms.horizon.events.traps.impl;

import lombok.Setter;
import org.opennms.horizon.events.traps.EventFactory;
import org.opennms.horizon.events.traps.TrapLogProtoToEventLogXmlMapper;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.function.Function;

@Component
public class TrapLogProtoToEventLogXmlMapperImpl implements TrapLogProtoToEventLogXmlMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TrapLogProtoToEventLogXmlMapperImpl.class);

    @Autowired
    @Setter
    private EventFactory eventFactory;

    // Testability
    @Setter
    private Function<String, InetAddress> inetAddressLookupFunction = InetAddressUtils::getInetAddress;

    @Override
    public Log convert(TenantLocationSpecificTrapLogDTO messageLog) {
        Log log = new Log();
        Events events = new Events();
        log.setEvents(events);

        String tenantId = messageLog.getTenantId();
        String location = messageLog.getLocation();

        // TODO: Add metrics for Traps received/error/dropped.
        for (TrapDTO eachMessage : messageLog.getTrapDTOList()) {
            try {
                var event =
                    eventFactory.createEventFrom(
                        eachMessage,
                        messageLog.getIdentity().getSystemId(),
                        location,
                        inetAddressLookupFunction.apply(messageLog.getTrapAddress()),
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
