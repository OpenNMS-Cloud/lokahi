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
package org.opennms.miniongateway.grpc.server.kafka;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Message;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.springframework.kafka.core.KafkaTemplate;

public class SinkMessageKafkaPublisherTest {

    public static final String TEST_TENANT_ID = "opennms-opti-prime";
    public static final String TEST_LOCATION_ID = "location-uuid-0x01";
    public static final String TEST_TOPIC_NAME = "flowable";
    private final TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor = mock(TenantIDGrpcServerInterceptor.class);

    private final LocationServerInterceptor locationServerInterceptor = mock(LocationServerInterceptor.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
    private final SinkMessageMapper<Message, Message> mapper = mock(SinkMessageMapper.class);
    private SinkMessageKafkaPublisher<Message, Message> messagePublisher;

    @Before
    public void setUp() {
        messagePublisher = new SinkMessageKafkaPublisher<>(
                kafkaTemplate,
                tenantIDGrpcInterceptor,
                locationServerInterceptor,
                mapper,
                TEST_TOPIC_NAME,
                new SimpleMeterRegistry());
    }

    @Test
    public void testContextLookup() {
        Mockito.when(tenantIDGrpcInterceptor.readCurrentContextTenantId()).thenReturn(TEST_TENANT_ID);
        Mockito.when(locationServerInterceptor.readCurrentContextLocationId()).thenReturn(TEST_LOCATION_ID);

        var flowsLog = FlowDocumentLog.newBuilder().build();

        // simulate enrichment of payload
        var expectedFlowDocumentLog = TenantLocationSpecificFlowDocumentLog.newBuilder()
                .setLocationId(TEST_LOCATION_ID)
                .setTenantId(TEST_TENANT_ID)
                .build();

        when(mapper.map(TEST_TENANT_ID, TEST_LOCATION_ID, flowsLog)).thenReturn(expectedFlowDocumentLog);

        messagePublisher.send(flowsLog);
        verify(mapper).map(TEST_TENANT_ID, TEST_LOCATION_ID, flowsLog);
        verify(kafkaTemplate).send(argThat(new ProducerRecordMatcher(TEST_TOPIC_NAME, expectedFlowDocumentLog)));
        verify(tenantIDGrpcInterceptor).readCurrentContextTenantId();
        verify(locationServerInterceptor).readCurrentContextLocationId();
    }

    static class ProducerRecordMatcher implements ArgumentMatcher<ProducerRecord<String, byte[]>> {

        private final String topic;
        private final Message payload;

        public ProducerRecordMatcher(String topic, Message payload) {
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        public boolean matches(ProducerRecord<String, byte[]> record) {
            return topic.equals(record.topic()) && Arrays.equals(payload.toByteArray(), record.value());
        }
    }
}
