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

import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.mapper.TenantLocationSpecificHeartbeatMessageMapper;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Forwarder of Heartbeat messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class HeartbeatKafkaForwarder implements MessageConsumer<HeartbeatMessage, HeartbeatMessage> {
    public static final String DEFAULT_HEARTBEAT_RESULTS_TOPIC = "heartbeat";
    private final SinkMessageKafkaPublisher<HeartbeatMessage, TenantLocationSpecificHeartbeatMessage> kafkaPublisher;

    @Autowired
    public HeartbeatKafkaForwarder(
            SinkMessageKafkaPublisherFactory messagePublisherFactory,
            TenantLocationSpecificHeartbeatMessageMapper mapper,
            @Value("${heartbeat.results.kafka-topic:" + DEFAULT_HEARTBEAT_RESULTS_TOPIC + "}") String kafkaTopic) {
        this.kafkaPublisher = messagePublisherFactory.create(mapper::mapBareToTenanted, kafkaTopic);
    }

    @Override
    public SinkModule<HeartbeatMessage, HeartbeatMessage> getModule() {
        return new HeartbeatModule();
    }

    @Override
    public void handleMessage(HeartbeatMessage message) {
        this.kafkaPublisher.send(message);
    }
}
