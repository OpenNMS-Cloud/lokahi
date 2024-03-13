package org.opennms.miniongateway.grpc.server.syslog;

import org.opennms.horizon.grpc.heartbeat.contract.mapper.TenantLocationSpecificHeartbeatMessageMapper;
import org.opennms.horizon.grpc.heartbeat.contract.mapper.impl.TenantLocationSpecificHeartbeatMessageMapperImpl;
import org.opennms.horizon.shared.grpc.syslog.contract.mapper.TenantLocationSpecificSysLogDTOMapper;
import org.opennms.horizon.shared.grpc.syslog.contract.mapper.impl.TenantLocationSpecificSysLogDTOMapperImpl;
import org.opennms.horizon.shared.grpc.traps.contract.mapper.TenantLocationSpecificTrapLogDTOMapper;
import org.opennms.horizon.shared.grpc.traps.contract.mapper.impl.TenantLocationSpecificTrapLogDTOMapperImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



    @Configuration
    public class SysLogApplicationConfig {



        @Bean
        public TenantLocationSpecificSysLogDTOMapper tenantLocationSpecificSysLogDTOMapper() {
            return new TenantLocationSpecificSysLogDTOMapperImpl();
        }
        @Bean
        public TenantLocationSpecificHeartbeatMessageMapper tenantLocationSpecificHeartbeatMessageMapper() {
            return new TenantLocationSpecificHeartbeatMessageMapperImpl();
        }

    }
