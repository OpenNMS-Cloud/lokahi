package org.opennms.horizon.grpc.heartbeat.contract.mapper;

import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;

public interface TenantLocationSpecificHeartbeatMessageMapper {
    TenantLocationSpecificHeartbeatMessage mapBareToTenanted(String tenantId, String location, HeartbeatMessage bare);
    HeartbeatMessage mapTenantedToBare(TenantLocationSpecificHeartbeatMessage tenantLocationSpecificHeartbeatMessage);
}
