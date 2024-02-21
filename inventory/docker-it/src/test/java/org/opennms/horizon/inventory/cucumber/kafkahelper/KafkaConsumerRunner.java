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
package org.opennms.horizon.inventory.cucumber.kafkahelper;

import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConsumerRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerRunner.class);
    private final String topic;
    private final KafkaConsumer<String, byte[]> consumer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final List<byte[]> values = Collections.synchronizedList(new LinkedList<>());

    public KafkaConsumerRunner(String bootStrapUrl, String topic) {
        this.topic = topic;
        Properties consumerConfig = new Properties();
        consumerConfig.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapUrl);
        consumerConfig.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerConfig.setProperty(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        consumerConfig.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "inventory-test");
        consumerConfig.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        consumerConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        this.consumer = new KafkaConsumer<>(consumerConfig);
    }

    public void shutdown() {
        closed.set(true);
    }

    @Override
    public void run() {

        consumer.subscribe(Collections.singletonList(topic));
        while (!closed.get()) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS));
            for (ConsumerRecord<String, byte[]> record : records) {
                this.values.add(record.value());
                try {
                    var tasksRequest = UpdateTasksRequest.parseFrom(record.value());
                    LOG.info("Consuming record {}", tasksRequest);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        }
        consumer.close();
        shutdown.set(true);
        LOG.info("Consumer closed");
    }

    public List<byte[]> getValues() {
        return new LinkedList<>(this.values);
    }

    public void clearValues() {
        this.values.clear();
    }

    public AtomicBoolean isShutdown() {
        return this.shutdown;
    }
}
