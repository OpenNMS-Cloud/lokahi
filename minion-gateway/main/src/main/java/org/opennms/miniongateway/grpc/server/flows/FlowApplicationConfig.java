package org.opennms.miniongateway.grpc.server.flows;

import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentLogMapper;
import org.opennms.horizon.shared.flows.mapper.impl.TenantLocationSpecificFlowDocumentLogMapperImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowApplicationConfig {
    @Bean
    public TenantLocationSpecificFlowDocumentLogMapper tenantLocationSpecificFlowDocumentLogMapper() {
        return new TenantLocationSpecificFlowDocumentLogMapperImpl();
    }
}
