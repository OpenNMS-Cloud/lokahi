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
package org.opennms.horizon.events.traps;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.springframework.stereotype.Component;

@Component
public class TrapsProducer {
    private final Producer<String, byte[]> producer;
    private final String topic;

    public TrapsProducer(String bootstrapServers, String topic) {
        this.topic = topic;
        this.producer = createKafkaProducer(bootstrapServers);
    }

    private Producer<String, byte[]> createKafkaProducer(String bootstrapServers) {
        // Configure producer properties
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        // Create Kafka producer
        return new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    }

    public void produce(TenantLocationSpecificTrapLogDTO obj) {
        // Create a Kafka record with the data and send it to the topic
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, obj.toByteArray());
        producer.send(record);
    }

    public void close() {
        // Close the Kafka producer when done
        producer.close();
    }
}
