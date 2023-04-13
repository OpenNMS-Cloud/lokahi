package org.opennms.horizon.flows.processing;

import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;

public interface FlowDocumentClassificationRequestMapper {
    ClassificationRequest createClassificationRequest(TenantLocationSpecificFlowDocument document);
}
