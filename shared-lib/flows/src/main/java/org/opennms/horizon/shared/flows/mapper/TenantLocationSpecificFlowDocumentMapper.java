package org.opennms.horizon.shared.flows.mapper;

import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;

public interface TenantLocationSpecificFlowDocumentMapper {
    TenantLocationSpecificFlowDocument mapBareToTenanted(String tenantId, String location, FlowDocument flowDocument);
    FlowDocument mapTenantedToBare(TenantLocationSpecificFlowDocument tenantLocationSpecificFlowDocument);
}
