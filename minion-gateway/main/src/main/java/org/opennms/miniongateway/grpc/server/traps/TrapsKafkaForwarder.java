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
package org.opennms.miniongateway.grpc.server.traps;

import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.grpc.traps.contract.mapper.TenantLocationSpecificTrapLogDTOMapper;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Forwarder of Trap messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class TrapsKafkaForwarder implements MessageConsumer<TrapLogDTO, TrapLogDTO> {

    public static final String DEFAULT_TRAP_RESULTS_TOPIC = "traps";

    private final SinkMessageKafkaPublisher<TrapLogDTO, TenantLocationSpecificTrapLogDTO> kafkaPublisher;

    @Autowired
    public TrapsKafkaForwarder(
            SinkMessageKafkaPublisherFactory messagePublisherFactory,
            TenantLocationSpecificTrapLogDTOMapper mapper,
            @Value("${traps.results.kafka-topic:" + DEFAULT_TRAP_RESULTS_TOPIC + "}") String kafkaTopic) {
        this.kafkaPublisher = messagePublisherFactory.create(mapper::mapBareToTenanted, kafkaTopic);
    }

    @Override
    public SinkModule<TrapLogDTO, TrapLogDTO> getModule() {
        return new TrapSinkModule();
    }

    @Override
    public void handleMessage(TrapLogDTO message) {
        this.kafkaPublisher.send(message);
    }
}
