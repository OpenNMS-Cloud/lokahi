package org.opennms.horizon.events.traps;

import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;

public interface TrapLogProtoToEventLogXmlMapper {
    Log convert(TenantLocationSpecificTrapLogDTO messageLog);
}
