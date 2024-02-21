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
package org.opennms.miniongateway.grpc.server.heartbeat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.mapper.TenantLocationSpecificHeartbeatMessageMapper;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageMapper;

@RunWith(MockitoJUnitRunner.class)
public class HeartbeatKafkaForwarderTest {

    private final String kafkaTopic = "kafkaTopic";

    @Mock
    private SinkMessageKafkaPublisherFactory publisherFactory;

    @Mock
    private TenantLocationSpecificHeartbeatMessageMapper mapper;

    @Mock
    private SinkMessageKafkaPublisher<Message, Message> publisher;

    private HeartbeatKafkaForwarder heartbeatForwarder;

    @Before
    public void setUp() {
        when(publisherFactory.create(any(SinkMessageMapper.class), eq(kafkaTopic)))
                .thenReturn(publisher);
        heartbeatForwarder = new HeartbeatKafkaForwarder(publisherFactory, mapper, kafkaTopic);
    }

    @Test
    public void testForward() {
        var message = HeartbeatMessage.newBuilder()
                .setIdentity(Identity.newBuilder().setSystemId("foo").build())
                .build();

        heartbeatForwarder.handleMessage(message);
        Mockito.verify(publisher).send(message);
    }
}
