/*
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
 *
 */

package org.opennms.miniongateway.grpc.server.tasktresults;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentLogMapper;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.protobuf.mapper.TenantLocationSpecificTaskSetResultsMapper;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageMapper;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskResultsKafkaForwarderTest {

    private final String kafkaTopic = "kafkaTopic";

    @Mock
    private SinkMessageKafkaPublisherFactory publisherFactory;
    @Mock
    private TenantLocationSpecificTaskSetResultsMapper mapper;
    @Mock
    private SinkMessageKafkaPublisher<Message, Message> publisher;

    private TaskResultsKafkaForwarder taskResultsKafkaForwarder;

    private TenantLocationSpecificTaskSetResults testTenantLocationSpecificTaskSetResults;

    @Before
    public void setUp() throws Exception {
        when(publisherFactory.create(any(SinkMessageMapper.class), eq(kafkaTopic))).thenReturn(publisher);
        taskResultsKafkaForwarder = new TaskResultsKafkaForwarder(publisherFactory, mapper, kafkaTopic);
    }

    @Test
    public void testHandleMessage() throws InvalidProtocolBufferException {
        //
        // Setup Test Data and Interactions
        //

        TaskResult testTaskResult = TaskResult.newBuilder().build();

        TaskSetResults testTaskSetResults = TaskSetResults.newBuilder()
            .addResults(testTaskResult)
            .build();
        //
        // Execute
        //
        taskResultsKafkaForwarder.handleMessage(testTaskSetResults);
        Mockito.verify(publisher).send(testTaskSetResults);
    }
}
