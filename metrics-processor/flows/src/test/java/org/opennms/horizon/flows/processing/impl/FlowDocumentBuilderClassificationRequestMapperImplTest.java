package org.opennms.horizon.flows.processing.impl;

import com.google.protobuf.UInt32Value;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.classification.IpAddr;
import org.opennms.horizon.flows.classification.persistence.api.Protocol;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;

import java.util.function.Function;

public class FlowDocumentBuilderClassificationRequestMapperImplTest {

    private Function<Integer, Protocol> mockProtocolLookupOp;

    private FlowDocumentClassificationRequestMapperImpl target;

    @BeforeEach
    public void setUp() {
        mockProtocolLookupOp = Mockito.mock(Function.class);

        target = new FlowDocumentClassificationRequestMapperImpl();

        target.setProtocolLookupOp(mockProtocolLookupOp);
    }

    @Test
    void testCreateClassificationRequest() {
        //
        // Setup Test Data and Interactions
        //
        var testDocument = TenantLocationSpecificFlowDocument.newBuilder()
            .setSrcAddress("1.1.1.1")
            .setSrcPort(UInt32Value.of(1))
            .setDstAddress("2.2.2.2")
            .setDstPort(UInt32Value.of(2))
            .setProtocol(UInt32Value.of(6));

        //
        // Execute
        //
        ClassificationRequest result = target.createClassificationRequest(testDocument.build());

        //
        // Verify the Results
        //
        Assert.assertEquals(IpAddr.of("1.1.1.1"), result.getSrcAddress());
        Assert.assertEquals(IpAddr.of("2.2.2.2"), result.getDstAddress());
        Assert.assertEquals(Integer.valueOf(1), result.getSrcPort());
        Assert.assertEquals(Integer.valueOf(2), result.getDstPort());
    }

    @Test
    void testCreateClassificationRequestMinimal() {
        //
        // Setup Test Data and Interactions
        //
        var testDocument = TenantLocationSpecificFlowDocument.newBuilder();

        //
        // Execute
        //
        ClassificationRequest result = target.createClassificationRequest(testDocument.build());

        //
        // Verify the Results
        //
        Assert.assertEquals(IpAddr.of("127.0.0.1"), result.getSrcAddress());
        Assert.assertEquals(IpAddr.of("127.0.0.1"), result.getDstAddress());
        Assert.assertNull(result.getSrcPort());
        Assert.assertNull(result.getDstPort());
    }
}
