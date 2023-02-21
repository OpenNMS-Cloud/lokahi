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

import io.grpc.Context;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class MinionHeartbeatConsumer {
    private final MonitoringSystemService service;
    private final MinionRpcManager minionRpc;
    @KafkaListener(topics = "${kafka.topics.minion-heartbeat}", concurrency = "1")
    public void receiveMessage(@Payload byte[] data, @Headers Map<String, Object> headers) {
        try {
            HeartbeatMessage message = HeartbeatMessage.parseFrom(data);
            String tenantId = Optional.ofNullable(headers.get(GrpcConstants.TENANT_ID_KEY)).map(o -> new String((byte[])o))
                .orElseThrow(()->new InventoryRuntimeException("Missing tenant id"));
            log.info("Received heartbeat message for minion with tenant id: {}; id: {}; location: {}", tenantId, message.getIdentity().getSystemId(), message.getIdentity().getLocation());
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
            {
                Optional<Long> newCreatedId = service.addMonitoringSystemFromHeartbeat(message, tenantId);
                newCreatedId.ifPresent(minionRpc::addSystem);
            });
        } catch (Exception e) {
            log.error("Error while processing heartbeat message: ", e);
        }
    }
}
