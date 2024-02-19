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
package org.opennms.miniongateway.grpc.server.flows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.protobuf.Message;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentLogMapper;
import org.opennms.horizon.shared.grpc.interceptor.MeteringServerInterceptor;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageMapper;

@RunWith(MockitoJUnitRunner.class)
public class FlowKafkaForwarderTest {
    private final String kafkaTopic = "kafkaTopic";

    @Mock
    private SinkMessageKafkaPublisherFactory publisherFactory;

    @Mock
    private TenantLocationSpecificFlowDocumentLogMapper mapper;

    @Mock
    private SinkMessageKafkaPublisher<Message, Message> publisher;

    private FlowKafkaForwarder flowKafkaForwarder;

    private MeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    @Before
    public void setUp() {
        when(publisherFactory.create(any(SinkMessageMapper.class), eq(kafkaTopic)))
                .thenReturn(publisher);
        flowKafkaForwarder = new FlowKafkaForwarder(publisherFactory, mapper, kafkaTopic, meterRegistry);
    }

    @Test
    public void testForward() {
        var message = FlowDocumentLog.newBuilder()
                .setSystemId("systemId")
                .addMessage(FlowDocument.newBuilder().setSrcAddress("127.0.0.1"))
                .addMessage(FlowDocument.newBuilder().setSrcAddress("0.0.0.0"))
                .build();

        flowKafkaForwarder.handleMessage(message);
        Mockito.verify(publisher).send(message);
        var flowCounter = meterRegistry.counter(
                FlowKafkaForwarder.class.getName(),
                MeteringServerInterceptor.SERVICE_TAG_NAME,
                FlowKafkaForwarder.FLOW_DOCUMENT_TAG);
        var flowLogCounter = meterRegistry.counter(
                FlowKafkaForwarder.class.getName(),
                MeteringServerInterceptor.SERVICE_TAG_NAME,
                FlowKafkaForwarder.FLOW_DOCUMENT_LOG_TAG);
        Assert.assertEquals(message.getMessageCount(), (int) flowCounter.count());
        Assert.assertEquals(1, (int) flowLogCounter.count());
    }
}
