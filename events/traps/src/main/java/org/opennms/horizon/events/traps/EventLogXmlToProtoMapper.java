package org.opennms.horizon.events.traps;

import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.xml.Log;

public interface EventLogXmlToProtoMapper {
    EventLog convert(Log eventLog, String tenantId);
}
