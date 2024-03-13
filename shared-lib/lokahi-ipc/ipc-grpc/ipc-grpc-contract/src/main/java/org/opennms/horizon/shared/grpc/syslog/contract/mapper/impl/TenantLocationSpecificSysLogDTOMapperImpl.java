package org.opennms.horizon.shared.grpc.syslog.contract.mapper.impl;

import org.opennms.horizon.grpc.syslog.contract.SyslogMessageLogDTO;
import org.opennms.horizon.grpc.syslog.contract.TenantLocationSpecificSysLogDTO;
import org.opennms.horizon.shared.grpc.syslog.contract.mapper.TenantLocationSpecificSysLogDTOMapper;


public class TenantLocationSpecificSysLogDTOMapperImpl implements TenantLocationSpecificSysLogDTOMapper {

    @Override
    public TenantLocationSpecificSysLogDTO mapBareToTenanted(String tenantId, String locationId, SyslogMessageLogDTO bare) {

        var result = TenantLocationSpecificSysLogDTO.newBuilder()
            .setTenantId(tenantId)
            .setLocationId(locationId)
            .setSysLogAddress(bare.getSourceAddress())
            .addAllSysLogDTO(bare.getMessagesList())
            .build();

        return result;
    }

    @Override
    public SyslogMessageLogDTO mapTenantedToBare(TenantLocationSpecificSysLogDTO tenantLocationSpecific) {
        var result = SyslogMessageLogDTO.newBuilder()
            .addAllMessages(tenantLocationSpecific.getSysLogDTOList())
            .setSourceAddress(tenantLocationSpecific.getSysLogAddress())
             .build();

        return result;
    }
}
