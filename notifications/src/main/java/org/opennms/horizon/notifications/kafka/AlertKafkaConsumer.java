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
package org.opennms.horizon.notifications.kafka;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class AlertKafkaConsumer {
    private final Logger LOG = LoggerFactory.getLogger(AlertKafkaConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${horizon.kafka.alerts.topic}", concurrency = "${horizon.kafka.alerts.concurrency}")
    public void consume(@Payload byte[] data) {
        try {
            Alert alert = Alert.parseFrom(data);
            if (Strings.isNullOrEmpty(alert.getTenantId())) {
                LOG.warn("TenantId is empty, dropping alert {}", alert);
                return;
            }
            notificationService.postNotification(alert);
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing Alert. Payload: {}", Arrays.toString(data), e);
        }
    }
}
