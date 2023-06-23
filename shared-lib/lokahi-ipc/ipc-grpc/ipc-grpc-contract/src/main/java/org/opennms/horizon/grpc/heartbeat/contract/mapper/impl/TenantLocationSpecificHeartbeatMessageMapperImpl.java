package org.opennms.horizon.grpc.heartbeat.contract.mapper.impl;

import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.mapper.TenantLocationSpecificHeartbeatMessageMapper;

public class TenantLocationSpecificHeartbeatMessageMapperImpl implements TenantLocationSpecificHeartbeatMessageMapper {
    @Override
    public TenantLocationSpecificHeartbeatMessage mapBareToTenanted(String tenantId, String locationId, HeartbeatMessage bare) {
        TenantLocationSpecificHeartbeatMessage result =
            TenantLocationSpecificHeartbeatMessage.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(locationId)
                .setIdentity(bare.getIdentity())
                .setTimestamp(bare.getTimestamp())
                .build();

        return result;
    }

    @Override
    public HeartbeatMessage mapTenantedToBare(TenantLocationSpecificHeartbeatMessage tenantLocationSpecificHeartbeatMessage) {
        HeartbeatMessage result =
            HeartbeatMessage.newBuilder()
                .setIdentity(tenantLocationSpecificHeartbeatMessage.getIdentity())
                .setTimestamp(tenantLocationSpecificHeartbeatMessage.getTimestamp())
                .build();

        return result;
    }
}
