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

package org.opennms.horizon.alarmservice.service.routing;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alarmservice.api.AlarmService;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
@PropertySource("classpath:application.yaml")
public class EventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private AlarmService alarmService;

    @KafkaListener(topics = "${kafka.topics.alarm-events}", concurrency = "1")
    public void receiveMessage(@Payload byte[] data, @Headers Map<String, Object> headers) {
        try {
            Event event = Event.parseFrom(data);
            log.info("Received alarm event message");

            var tenantOptional = getTenantId(headers);
            if (tenantOptional.isEmpty()) {
                LOG.warn("TenantId is empty, dropping event {}", event);
                return;
            }
            String tenantId = tenantOptional.get();

            // As this isn't a grpc call, there isn't a grpc context. Create one, and place the tenantId in it.
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
            {
                alarmService.process(event);
            });
        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing Event message", e);
        }
    }

    private Optional<String> getTenantId(Map<String, Object> headers) {
        Object tenantId = headers.get(GrpcConstants.TENANT_ID_KEY);
        if (tenantId instanceof byte[]) {
            return Optional.of(new String((byte[]) tenantId));
        }
        return Optional.empty();
    }
}
