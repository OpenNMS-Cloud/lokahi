/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.notifications.kafka;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.notifications.service.MonitoringPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class MonitoringPolicyKafkaConsumer {
    private final Logger LOG = LoggerFactory.getLogger(MonitoringPolicyKafkaConsumer.class);

    @Autowired
    private MonitoringPolicyService monitoringPolicyService;

    @KafkaListener(
        topics = "${horizon.kafka.monitoring-policy.topic}",
        concurrency = "${horizon.kafka.monitoring-policy.concurrency}"
    )
    public void consume(@Payload byte[] data) {
        try {
            MonitorPolicyProto monitoringPolicyProto = MonitorPolicyProto.parseFrom(data);
            if (Strings.isNullOrEmpty(monitoringPolicyProto.getTenantId())) {
                LOG.warn("TenantId is empty, dropping alert {}", monitoringPolicyProto);
                return;
            }
            monitoringPolicyService.saveMonitoringPolicy(monitoringPolicyProto);
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing Monitoring Policy. Payload: {}", Arrays.toString(data), e);
        }
    }
}
