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

package org.opennms.horizon.inventory.component;

import java.util.Map;
import java.util.Optional;

import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.inventory.Constants;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class MinionHeartbeatConsumer {
    private final MonitoringSystemService service;
    @KafkaListener(topics = "${kafka.topics.minion-heartbeat}", concurrency = "1")
    public void receiveMessage(@Payload byte[] data, @Headers Map<String, Object> headers) {
        try {
            HeartbeatMessage message = HeartbeatMessage.parseFrom(data);
            log.info("Received heartbeat message for minion with id {} and location {}", message.getIdentity().getSystemId(), message.getIdentity().getLocation());
            String tenantId = Optional.ofNullable(headers.get(Constants.TENANT_ID_KEY)).map(o -> new String((byte[])o)).orElse(Constants.DEFAULT_TENANT_ID);
            service.addMonitoringSystemFromHeartbeat(message, tenantId);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing heartbeat message", e);
        }
    }
}
