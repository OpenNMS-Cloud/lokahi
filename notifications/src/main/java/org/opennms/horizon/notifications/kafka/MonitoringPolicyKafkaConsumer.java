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
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.notifications.service.MonitoringPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class MonitoringPolicyKafkaConsumer {
    private final Logger LOG = LoggerFactory.getLogger(MonitoringPolicyKafkaConsumer.class);

    @Autowired
    private MonitoringPolicyService monitoringPolicyService;

    @KafkaListener(
            topics = "${horizon.kafka.monitoring-policy.topic}",
            concurrency = "${horizon.kafka.monitoring-policy.concurrency}")
    public void consume(@Payload byte[] data) {
        try {
            MonitorPolicyProto monitoringPolicyProto = MonitorPolicyProto.parseFrom(data);
            if (Strings.isNullOrEmpty(monitoringPolicyProto.getTenantId())) {
                LOG.warn("TenantId is empty, dropping Monitoring Policy {}", monitoringPolicyProto);
                return;
            }
            monitoringPolicyService.saveMonitoringPolicy(monitoringPolicyProto);
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing Monitoring Policy. Payload: {}", Arrays.toString(data), e);
        }
    }
}
