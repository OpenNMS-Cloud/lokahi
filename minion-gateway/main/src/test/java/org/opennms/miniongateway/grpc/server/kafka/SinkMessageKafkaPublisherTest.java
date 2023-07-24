/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.miniongateway.grpc.server.kafka;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Message;
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

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
            kafkaTemplate, tenantIDGrpcInterceptor, locationServerInterceptor, mapper, TEST_TOPIC_NAME, new SimpleMeterRegistry()
        );
    }

    @Test
    public void testContextLookup() {
        Mockito.when(tenantIDGrpcInterceptor.readCurrentContextTenantId()).thenReturn(TEST_TENANT_ID);
        Mockito.when(locationServerInterceptor.readCurrentContextLocationId()).thenReturn(TEST_LOCATION_ID);

        var flowsLog = FlowDocumentLog.newBuilder()
            .build();

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
