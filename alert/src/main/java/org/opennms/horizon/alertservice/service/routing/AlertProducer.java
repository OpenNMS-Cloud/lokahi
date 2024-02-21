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
package org.opennms.horizon.alertservice.service.routing;

import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alertservice.api.AlertLifecycleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.config.KafkaTopicProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertProducer implements AlertLifecycleListener {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private final AlertService alertService;

    private final String kafkaTopic;

    public AlertProducer(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            AlertService alertService,
            KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.alertService = Objects.requireNonNull(alertService);
        this.kafkaTopic = kafkaTopicProperties.getAlert();
    }

    @PostConstruct
    public void init() {
        alertService.addListener(this);
    }

    @PreDestroy
    public void destroy() {
        alertService.removeListener(this);
    }

    @Override
    public void handleNewOrUpdatedAlert(Alert alert) {
        var producerRecord = new ProducerRecord<>(kafkaTopic, toKey(alert), alert.toByteArray());
        kafkaTemplate.send(producerRecord);
    }

    @Override
    public void handleDeletedAlert(Alert alert) {
        var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, toKey(alert), null);
        kafkaTemplate.send(producerRecord);
    }

    private String toKey(Alert alert) {
        return alert.getTenantId() + "-" + alert.getLocation();
    }
}
