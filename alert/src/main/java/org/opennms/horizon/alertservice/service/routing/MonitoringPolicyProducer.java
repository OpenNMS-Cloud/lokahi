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

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alertservice.config.KafkaTopicProperties;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("JpaEntityListenerInspection") // no-args constructor not required since Hibernate 5.3 and Spring 5.1
public class MonitoringPolicyProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private final String kafkaTopic;

    public MonitoringPolicyProducer(
            KafkaTemplate<String, byte[]> kafkaTemplate, KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTopic = kafkaTopicProperties.getMonitoringPolicy();
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostUpdate
    @PostPersist
    public void sendMonitoringPolicy(MonitorPolicy monitorPolicy) {
        // Not all fields are included in this proto, since the Notification service doesn't care about all of them.
        MonitorPolicyProto proto = MonitorPolicyProto.newBuilder()
                .setId(monitorPolicy.getId())
                .setTenantId(monitorPolicy.getTenantId())
                .setNotifyByEmail(monitorPolicy.getNotifyByEmail())
                .setNotifyByWebhooks(monitorPolicy.getNotifyByWebhooks())
                .setNotifyByPagerDuty(monitorPolicy.getNotifyByPagerDuty())
                .build();

        var record = new ProducerRecord<>(kafkaTopic, toKey(monitorPolicy), proto.toByteArray());
        kafkaTemplate.send(record);
    }

    private String toKey(MonitorPolicy monitorPolicy) {
        return monitorPolicy.getId().toString();
    }
}
