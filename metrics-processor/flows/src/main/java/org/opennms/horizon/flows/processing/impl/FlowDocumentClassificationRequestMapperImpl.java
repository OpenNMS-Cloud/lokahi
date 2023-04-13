package org.opennms.horizon.flows.processing.impl;

import lombok.Setter;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.classification.persistence.api.Protocol;
import org.opennms.horizon.flows.classification.persistence.api.Protocols;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;
import org.opennms.horizon.flows.processing.FlowDocumentClassificationRequestMapper;

import java.util.function.Function;

public class FlowDocumentClassificationRequestMapperImpl implements FlowDocumentClassificationRequestMapper {

    // Testability (TODO: stop using static methods in Protocols and replace this Op with an injected instance)
    @Setter
    private Function<Integer, Protocol> protocolLookupOp = Protocols::getProtocol;

    @Override
    public ClassificationRequest createClassificationRequest(TenantLocationSpecificFlowDocument document) {
        ClassificationRequest request = new ClassificationRequest();
        if (document.hasProtocol()) {
            request.setProtocol(protocolLookupOp.apply(document.getProtocol().getValue()));
        }
        request.setLocation(document.getLocation());
        request.setExporterAddress(document.getHost());
        request.setDstAddress(document.getDstAddress());
        if (document.hasDstPort()) {
            request.setDstPort(document.getDstPort().getValue());
        }
        request.setSrcAddress(document.getSrcAddress());
        if (document.hasSrcPort()) {
            request.setSrcPort(document.getSrcPort().getValue());
        }

        return request;
    }
}
