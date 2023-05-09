/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
 */

package org.opennms.horizon.inventory.cucumber.kafkahelper;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.inventory.cucumber.kafkahelper.internals.KafkaProcessor;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class KafkaTestHelper {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaTestHelper.class);

    private static int unique = 0;

    @Getter
    @Setter
    private String kafkaBootstrapUrl;
    private KafkaProducer<String, byte[]> kafkaProducer;

    private final Object lock = new Object();

    private Map<String, KafkaProcessor<String, byte[]>> kafkaTopicProcessors = new HashMap<>();
    private Map<String, List<ConsumerRecord<String, byte[]>>> consumedRecords = new HashMap<>();

//========================================
// Test Operations
//----------------------------------------

    public void startConsumerAndProducer(String consumerTopic, String producerTopic) {
        try {
            KafkaConsumer<String, byte[]> consumer = this.createKafkaConsumer("test-consumer-group"+ ++unique, "test-consumer-for-" + consumerTopic);
            kafkaProducer = this.createKafkaProducer();
            KafkaProcessor<String, byte[]> processor = new KafkaProcessor<>(consumer, kafkaProducer, records -> processRecords(consumerTopic, records));

            LOG.info("Adding consumer topic {}", consumerTopic);
            kafkaTopicProcessors.putIfAbsent(consumerTopic, processor);

            LOG.info("Adding producer topic {}", producerTopic);
            kafkaTopicProcessors.putIfAbsent(producerTopic, processor);
            
            consumer.subscribe(Collections.singletonList(consumerTopic));

            startPollingThread(processor);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void sendToTopic(String topic, byte[] body, String tenantId) {
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, body);
        producerRecord.headers().add(GrpcConstants.TENANT_ID_KEY, tenantId.getBytes());
        kafkaProducer.send(producerRecord);
    }

    public List<ConsumerRecord<String, byte[]>> getConsumedMessages(String topic)  {
        List<ConsumerRecord<String, byte[]>> result = new LinkedList<>();

        synchronized (lock) {
            List<ConsumerRecord<String, byte[]>> consumed = consumedRecords.get(topic);
            if (consumed != null) {
                result.addAll(consumed);
            }
        }

        return result;
    }

//========================================
// Kafka Client
//----------------------------------------

    private <K,V> KafkaConsumer<K,V> createKafkaConsumer(String groupId, String consumerName) {

        // create instance for properties to access producer configs
        Properties props = new Properties();

        props.put("group.id", groupId);
        props.put("group.instance.id", consumerName);

        //Assign localhost id
        props.put("bootstrap.servers", kafkaBootstrapUrl);

        //Set acknowledgements for producer requests.
        props.put("acks","all");

        //If the request fails, the producer can automatically retry,
        props.put("retries", 0);

        //Specify buffer size in config
        props.put("batch.size", 16384);

        //Reduce the no of requests less than 0
        props.put("linger.ms", 1);

        //The buffer.memory controls the total amount of memory available to the producer for buffering.
        props.put("buffer.memory", 33554432);

        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        props.put("auto.offset.reset", "earliest");

        return new KafkaConsumer<K, V>(props);
    }

    private <K,V> KafkaProducer<K,V> createKafkaProducer() {

        // create instance for properties to access producer configs
        Properties props = new Properties();


        //Assign localhost id
        props.put("bootstrap.servers", kafkaBootstrapUrl);

        //Set acknowledgements for producer requests.
        props.put("acks","all");

        //If the request fails, the producer can automatically retry,
        props.put("retries", 0);

        //Specify buffer size in config
        props.put("batch.size", 16384);

        //Reduce the no of requests less than 0
        props.put("linger.ms", 1);

        //The buffer.memory controls the total amount of memory available to the producer for buffering.
        props.put("buffer.memory", 33554432);

        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        return new KafkaProducer<K, V>(props);
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
