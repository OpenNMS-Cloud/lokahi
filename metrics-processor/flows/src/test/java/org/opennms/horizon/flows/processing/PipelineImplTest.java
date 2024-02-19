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
package org.opennms.horizon.flows.processing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.integration.FlowException;
import org.opennms.horizon.flows.integration.FlowRepository;

public class PipelineImplTest {
    private final DocumentEnricherImpl documentEnricher = mock(DocumentEnricherImpl.class);
    private final MetricRegistry metricRegistry = new MetricRegistry();

    private final FlowRepository flowRepository = mock(FlowRepository.class);
    private final PipelineImpl pipeline = new PipelineImpl(metricRegistry, documentEnricher);

    @Before
    public void setup() {
        var properties = new HashMap<>();
        properties.put(PipelineImpl.REPOSITORY_ID, "DataPlatform");
        pipeline.onBind(flowRepository, properties);
    }

    @Test
    public void testPipeline() throws FlowException {
        var flowsLog = TenantLocationSpecificFlowDocumentLog.newBuilder()
                .setLocationId("location")
                .setTenantId("tenantId")
                .setSystemId("systemId")
                .addMessage(FlowDocument.newBuilder().setSrcAddress("127.0.0.1"))
                .build();
        pipeline.process(flowsLog);
        Mockito.verify(documentEnricher, Mockito.times(1)).enrich(any(TenantLocationSpecificFlowDocumentLog.class));
        Mockito.verify(flowRepository, Mockito.times(1)).persist((any(TenantLocationSpecificFlowDocumentLog.class)));
    }
}
