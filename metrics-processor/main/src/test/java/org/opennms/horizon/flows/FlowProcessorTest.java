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
package org.opennms.horizon.flows;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;

@ExtendWith(MockitoExtension.class)
public class FlowProcessorTest {

    public static final String TENANT_ID = "the_big_brother";

    @Mock
    private Pipeline pipeline;

    @Mock
    private TenantMetricsTracker metricsTracker;

    private FlowProcessor processor;

    @BeforeEach
    public void setup() {
        processor = new FlowProcessor(pipeline, metricsTracker);
    }

    @Test
    void testFlowsSampling() throws Exception {
        TenantLocationSpecificFlowDocumentLog flows = TenantLocationSpecificFlowDocumentLog.newBuilder()
                .setTenantId(TENANT_ID)
                .addMessage(FlowDocument.newBuilder().setSrcAddress("127.0.0.1").setDstAddress("8.8.8.8"))
                .addMessage(
                        FlowDocument.newBuilder().setSrcAddress("192.168.0.1").setDstAddress("1.1.1.1"))
                .build();

        processor.consume(flows.toByteArray());

        verify(pipeline, timeout(5000).only()).process(flows);
        verify(metricsTracker, timeout(5000).times(1)).addTenantFlowReceviedCount(TENANT_ID, 2);
        verify(metricsTracker, timeout(5000).times(1)).addTenantFlowCompletedCount(TENANT_ID, 2);
    }
}
