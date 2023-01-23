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

package org.opennms.horizon.kafkahelper.internals;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;

public class KafkaProcessor<K,V> implements Runnable {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(KafkaProcessor.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final KafkaConsumer<K,V> consumer;
    private final Consumer<ConsumerRecords<K,V>> onRecords;

    private boolean shutdown = false;

    public KafkaProcessor(KafkaConsumer<K,V> consumer, Consumer<ConsumerRecords<K,V>> onRecords) {
        this.consumer = consumer;
        this.onRecords = onRecords;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    @Override
    public void run() {
        LOG.info("STARTING KAFKA CONSUMER POLL: topics={}", consumer.subscription());

        while (! shutdown) {
            try {
                ConsumerRecords<K,V> records = consumer.poll(Duration.ofMillis(100));

                if ((records != null) && (! records.isEmpty())) {
                    LOG.info("POLL returned records: count={}; topics={}", records.count(), consumer.subscription());
                    onRecords.accept(records);
                }
            } catch (Exception exc) {
                LOG.error("Error reading records from kafka", exc);

                // Add a delay between iterations to reduce spamming the logs when consumer.poll() fails immediately,
                //  such as at startup when Kafka is not yet fully started.
                delay();
            }
        }
    }

//========================================
// Internals
//----------------------------------------

    private void delay() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException interruptedException) {
            LOG.debug("delay interrupted", interruptedException);
        }
    }
}
