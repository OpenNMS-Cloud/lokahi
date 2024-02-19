/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.flows.classification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.flows.classification.persistence.api.Protocol;

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

    // ========================================
    // Internals
    // ----------------------------------------

    private void commonTestClassifiable(
            Integer srcPort, Integer dstPort, Protocol protocol, boolean expectedClassifiable) {
        var target = new ClassificationRequest();

        target.setSrcPort(srcPort);
        target.setDstPort(dstPort);
        target.setProtocol(protocol);

        assertEquals(expectedClassifiable, target.isClassifiable());
    }
}
