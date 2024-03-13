package org.opennms.horizon.shared.grpc.syslog.contract.mapper;

import org.opennms.horizon.grpc.syslog.contract.SyslogMessageLogDTO;
import org.opennms.horizon.grpc.syslog.contract.TenantLocationSpecificSysLogDTO;

public interface TenantLocationSpecificSysLogDTOMapper {

    TenantLocationSpecificSysLogDTO mapBareToTenanted(String tenantId, String locationId, SyslogMessageLogDTO bare);

    SyslogMessageLogDTO mapTenantedToBare(TenantLocationSpecificSysLogDTO tenantLocationSpecific);
}
