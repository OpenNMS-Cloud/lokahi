/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.alertservice.service.routing;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Context;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@RequiredArgsConstructor
@Component
@PropertySource("classpath:application.yaml")
public class NodeConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(NodeConsumer.class);
    private final AlertService alertService;
    @KafkaListener(topics = "${kafka.topics.node-changed}", concurrency = "1")
    public void receiveMessage(@Payload byte[] data) {
        try {
            NodeDTO node = NodeDTO.parseFrom(data);
            if (Strings.isNullOrEmpty(node.getTenantId())) {
                LOG.warn("TenantId is empty, dropping node {}", node);
                return;
            }

            // As this isn't a grpc call, there isn't a grpc context. Create one, and place the tenantId in it.
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, node.getTenantId()).run(()-> alertService.saveNode(node));
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing Node. Payload: {}", Arrays.toString(data), e);
        }
    }
}
