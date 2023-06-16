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

package org.opennms.horizon.minioncertverifier.grpc;

import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.MetadataUtils;
import java.util.Optional;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// TODO - Temporary work to make sure we exchange location-id to location-name needed by backend until HS-1647 is merged
@Component
@Profile("default")
public class BasicLocationClient implements LocationClient {

    private final Logger logger = LoggerFactory.getLogger(BasicLocationClient.class);
    private final MonitoringLocationServiceBlockingStub client;

    public BasicLocationClient(@Value("${grpc.url.inventory:opennms-inventory:6565}") String inventoryAddress) throws Exception {

        ManagedChannel channel = OkHttpChannelBuilder.forTarget(inventoryAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext()
            .build();

        this.client = MonitoringLocationServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public Optional<MonitoringLocationDTO> getLocation(String tenantId, long locationId) {
        try {
            return Optional.ofNullable(client.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(prepareGrpcHeaders(tenantId)))
                .getLocationById(Int64Value.of(locationId)));
        } catch (Exception e) {
            logger.warn("Could not obtain location information for tenantId={}; locationId={}", tenantId, locationId, e);
        }
        return Optional.empty();
    }

    private Metadata prepareGrpcHeaders(String tenantId) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, "");
        return metadata;
    }
}
