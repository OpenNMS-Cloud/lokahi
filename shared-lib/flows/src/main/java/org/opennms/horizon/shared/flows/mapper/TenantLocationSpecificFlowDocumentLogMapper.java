package org.opennms.horizon.shared.flows.mapper;

import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;

public interface TenantLocationSpecificFlowDocumentLogMapper {
    TenantLocationSpecificFlowDocumentLog mapBareToTenanted(String tenantId, String location, FlowDocumentLog flowDocumentLog);
    FlowDocumentLog mapTenantedToBare(TenantLocationSpecificFlowDocumentLog tenantLocationSpecificFlowDocumentLog);
}
