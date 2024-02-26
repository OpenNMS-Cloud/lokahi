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
package org.opennms.horizon.flows.processing.impl;

import com.google.protobuf.UInt32Value;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.classification.IpAddr;
import org.opennms.horizon.flows.classification.persistence.api.Protocol;
import org.opennms.horizon.flows.document.FlowDocument;

class FlowDocumentBuilderClassificationRequestMapperImplTest {

    private Function<Integer, Protocol> mockProtocolLookupOp;

    private FlowDocumentClassificationRequestMapperImpl target;

    private final String location = "location";

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
        var testDocument = FlowDocument.newBuilder()
                .setSrcAddress("1.1.1.1")
                .setSrcPort(UInt32Value.of(1))
                .setDstAddress("2.2.2.2")
                .setDstPort(UInt32Value.of(2))
                .setProtocol(UInt32Value.of(6));

        //
        // Execute
        //
        ClassificationRequest result = target.createClassificationRequest(testDocument.build(), location);

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
        var testDocument = FlowDocument.newBuilder();

        //
        // Execute
        //
        ClassificationRequest result = target.createClassificationRequest(testDocument.build(), location);

        //
        // Verify the Results
        //
        Assert.assertEquals(IpAddr.of("127.0.0.1"), result.getSrcAddress());
        Assert.assertEquals(IpAddr.of("127.0.0.1"), result.getDstAddress());
        Assert.assertNull(result.getSrcPort());
        Assert.assertNull(result.getDstPort());
    }
}
