package org.opennms.horizon.flows.classification;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.flows.classification.persistence.api.Protocol;

import static org.junit.jupiter.api.Assertions.*;

public class ClassificationRequestTest {

    // TODO: this test can be removed once the logic is removed from ClassificationRequest itself
    /**
     * Verify the logc in isClassifiable()
     */
    @Test
    void testClassificationRequestGetterLogic() {
        var testProtocol = new Protocol(1, "abc", "def");
        var target = new ClassificationRequest();

        // Verify the default state
        assertFalse(target.isClassifiable());

        // Verify that each term has the intended impact on the result
        commonTestClassifiable(123, 456, null, false);
        commonTestClassifiable(null, 456, testProtocol, false);
        commonTestClassifiable(123, null, testProtocol, false);

        // Verify the positive result
        commonTestClassifiable(123, 456, testProtocol, true);
    }

//========================================
// Internals
//----------------------------------------

    private void commonTestClassifiable(Integer srcPort, Integer dstPort, Protocol protocol, boolean expectedClassifiable) {
        var target = new ClassificationRequest();

        target.setSrcPort(srcPort);
        target.setDstPort(dstPort);
        target.setProtocol(protocol);

        assertEquals(expectedClassifiable, target.isClassifiable());
    }
}
