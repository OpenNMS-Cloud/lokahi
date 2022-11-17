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

package org.opennms.horizon.inventory.service.taskset.publisher;

import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class GrpcTaskSetPublisher implements TaskSetPublisher {
    private static final Logger log = LoggerFactory.getLogger(GrpcTaskSetPublisher.class);

    @Value("${grpc.client.minion-gateway.host:localhost}")
    private String host;

    @Value("${grpc.client.minion-gateway.port:8990}")
    private int port;

    @Value("${grpc.client.minion-gateway.tlsEnabled:false}")
    private boolean tlsEnabled;

    @Value("${grpc.client.minion-gateway.maxMessageSize:10485760}")
    private int maxMessageSize;

    private TaskSetServiceGrpc.TaskSetServiceBlockingStub taskSetServiceStub;

    @PostConstruct
    public void init() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port)
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(maxMessageSize);

        Channel channel;

        if (tlsEnabled) {
            throw new RuntimeException("TLS NOT YET IMPLEMENTED");
            /*
             channel = channelBuilder
                 .negotiationType(NegotiationType.TLS)
                 .sslContext(buildSslContext().build())
                 .build();
             log.info("TLS enabled for TaskSet gRPC");
             */
        } else {
            channel = channelBuilder.usePlaintext().build();
        }

        taskSetServiceStub = TaskSetServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void publishTaskSet(String location, TaskSet taskSet) {
        try {
            PublishTaskSetRequest request =
                PublishTaskSetRequest.newBuilder()
                    .setLocation(location)
                    .setTaskSet(taskSet)
                    .build();

            PublishTaskSetResponse response = taskSetServiceStub.publishTaskSet(request);

            log.debug("PUBLISH task set complete: location={}, response={}", location, response);
        } catch (Exception e) {
            log.error("Error publishing taskset", e);
            throw new RuntimeException("failed to publish taskset", e);
        }
    }
}
