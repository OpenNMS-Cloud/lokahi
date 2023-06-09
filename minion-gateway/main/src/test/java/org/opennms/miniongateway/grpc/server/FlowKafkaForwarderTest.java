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

package org.opennms.miniongateway.grpc.server;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentLogMapper;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.miniongateway.grpc.server.flows.FlowApplicationConfig;
import org.opennms.miniongateway.grpc.server.flows.FlowKafkaForwarder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.argThat;


@RunWith(MockitoJUnitRunner.class)
public class FlowKafkaForwarderTest {
    @InjectMocks
    private FlowKafkaForwarder flowKafkaForwarder = new FlowKafkaForwarder();

    @Mock
    private TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor;

    @Mock
    private LocationServerInterceptor locationServerInterceptor;

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private final FlowApplicationConfig flowApplicationConfig = new FlowApplicationConfig();
    @Spy
    private TenantLocationSpecificFlowDocumentLogMapper tenantLocationSpecificFlowDocumentLogMapper = flowApplicationConfig.tenantLocationSpecificFlowDocumentLogMapper();

    private final String kafkaTopic = "kafkaTopic";
    @Before
    public void setUp() {
        ReflectionTestUtils.setField(flowKafkaForwarder, "kafkaTopic", kafkaTopic);
    }

    @Captor
    ArgumentCaptor<ProducerRecord<String, byte[]>> captor;

    @Test
    public void testForward() throws InvalidProtocolBufferException {
        Mockito.when(tenantIDGrpcInterceptor.readCurrentContextTenantId()).thenReturn("tenantId");
        Mockito.when(locationServerInterceptor.readCurrentContextLocation()).thenReturn("location");
        var flowsLog = FlowDocumentLog.newBuilder()
            .setSystemId("systemId")
            .addMessage(FlowDocument.newBuilder()
                .setSrcAddress("127.0.0.1"))
            .build();

        var expectedFlowDocumentLog = TenantLocationSpecificFlowDocumentLog.newBuilder()
            .setSystemId("systemId")
            .setLocation("location")
            .setTenantId("tenantId")
            .addMessage(FlowDocument.newBuilder()
                .setSrcAddress("127.0.0.1"))
            .build();
        var expectedProducerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, expectedFlowDocumentLog.toByteArray());

        flowKafkaForwarder.handleMessage(flowsLog);

        class ProducerRecordMatcher implements ArgumentMatcher<ProducerRecord<String, byte[]>> {
            private final ProducerRecord<String, byte[]> left;

            public ProducerRecordMatcher(ProducerRecord<String, byte[]> left) {
                this.left = left;
            }

            @Override
            public boolean matches(ProducerRecord<String, byte[]> right) {
                return left.topic().equals(right.topic()) && Arrays.equals(left.value(), right.value());
            }
        }

        Mockito.verify(kafkaTemplate, Mockito.times(1)).send(argThat(new ProducerRecordMatcher(expectedProducerRecord)));
    }
}
