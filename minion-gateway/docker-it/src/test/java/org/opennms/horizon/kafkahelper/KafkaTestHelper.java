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
package org.opennms.horizon.kafkahelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.opennms.horizon.kafkahelper.internals.KafkaProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaTestHelper {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(KafkaTestHelper.class);

    private Logger LOG = DEFAULT_LOGGER;

    private String kafkaBootstrapUrl;

    private final Object lock = new Object();

    private Map<String, KafkaProcessor<String, byte[]>> kafkaTopicProcessors = new HashMap<>();
    private Map<String, List<ConsumerRecord<String, byte[]>>> consumedRecords = new HashMap<>();

    // ========================================
    // Getters and Setters
    // ----------------------------------------

    public String getKafkaBootstrapUrl() {
        return kafkaBootstrapUrl;
    }

    public void setKafkaBootstrapUrl(String kafkaBootstrapUrl) {
        this.kafkaBootstrapUrl = kafkaBootstrapUrl;
    }

    // ========================================
    // Test Operations
    // ----------------------------------------

    public boolean startConsumer(String topic) {
        try {
            KafkaConsumer<String, byte[]> consumer =
                    this.createKafkaConsumer("test-consumer-group", "test-consumer-for-" + topic);
            KafkaProcessor<String, byte[]> processor =
                    new KafkaProcessor<>(consumer, records -> processRecords(topic, records));

            synchronized (lock) {
                if (kafkaTopicProcessors.containsKey(topic)) {
                    return false;
                }

                kafkaTopicProcessors.put(topic, processor);
            }

            consumer.subscribe(Collections.singletonList(topic));

            startPollingThread(processor);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        return true;
    }

    public void removeConsumer(String topic) {
        KafkaProcessor<String, byte[]> processor;

        synchronized (lock) {
            processor = kafkaTopicProcessors.remove(topic);
        }

        if (processor != null) {
            processor.shutdown();
        }
    }

    public List<ConsumerRecord<String, byte[]>> getConsumedMessages(String topic) {
        List<ConsumerRecord<String, byte[]>> result = new LinkedList<>();

        synchronized (lock) {
            List<ConsumerRecord<String, byte[]>> consumed = consumedRecords.get(topic);
            if (consumed != null) {
                result.addAll(consumed);
            }
        }

        return result;
    }

    // ========================================
    // Kafka Client
    // ----------------------------------------

    private <K, V> KafkaConsumer<K, V> createKafkaConsumer(String groupId, String consumerName) {

        // create instance for properties to access producer configs
        Properties props = new Properties();

        props.put("group.id", groupId);
        props.put("group.instance.id", consumerName);

        // Assign localhost id
        props.put("bootstrap.servers", kafkaBootstrapUrl);

        // Set acknowledgements for producer requests.
        props.put("acks", "all");

        // If the request fails, the producer can automatically retry,
        props.put("retries", 0);

        // Specify buffer size in config
        props.put("batch.size", 16384);

        // Reduce the no of requests less than 0
        props.put("linger.ms", 1);

        // The buffer.memory controls the total amount of memory available to the producer for buffering.
        props.put("buffer.memory", 33554432);

        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        props.put("auto.offset.reset", "earliest");

        return new KafkaConsumer<K, V>(props);
    }

    private void processRecords(String topic, ConsumerRecords<String, byte[]> records) {
        synchronized (lock) {
            List<ConsumerRecord<String, byte[]>> recordList =
                    consumedRecords.computeIfAbsent(topic, key -> new LinkedList<>());
            records.forEach(recordList::add);
        }
    }

    private void startPollingThread(KafkaProcessor<String, byte[]> processor) {
        Thread processorThread = new Thread(processor);
        processorThread.start();
    }
}
