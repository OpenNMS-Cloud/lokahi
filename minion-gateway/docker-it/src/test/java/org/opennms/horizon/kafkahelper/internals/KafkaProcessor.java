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
package org.opennms.horizon.kafkahelper.internals;

import java.time.Duration;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaProcessor<K, V> implements Runnable {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(KafkaProcessor.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final KafkaConsumer<K, V> consumer;
    private final Consumer<ConsumerRecords<K, V>> onRecords;

    private boolean shutdown = false;

    public KafkaProcessor(KafkaConsumer<K, V> consumer, Consumer<ConsumerRecords<K, V>> onRecords) {
        this.consumer = consumer;
        this.onRecords = onRecords;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    @Override
    public void run() {
        LOG.info("STARTING KAFKA CONSUMER POLL: topics={}", consumer.subscription());

        while (!shutdown) {
            try {
                ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));

                if ((records != null) && (!records.isEmpty())) {
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

    // ========================================
    // Internals
    // ----------------------------------------

    private void delay() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException interruptedException) {
            LOG.debug("delay interrupted", interruptedException);
        }
    }
}
