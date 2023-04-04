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

package org.opennms.horizon.flows.grpc.client;

import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class InventoryClient {
    private final ManagedChannel channel;
    private final long deadline;
    private NodeServiceGrpc.NodeServiceBlockingStub nodeStub;

    protected void initialStubs() {
        nodeStub = NodeServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    private Metadata getMetadata(boolean bypassAuthorization, String tenantId){
        var metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(bypassAuthorization));
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        return metadata;
    }

    public NodeDTO getNodeById(long nodeId, String tenantId) {
        Metadata metadata = getMetadata(true, tenantId);

        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .getNodeById(Int64Value.of(nodeId));
    }

    public long getNodeIdFromQuery(String tenantId, String ipAddress, String location) {
        Metadata metadata = getMetadata(true, tenantId);

        NodeIdQuery query = NodeIdQuery.newBuilder()
            .setIpAddress(ipAddress).setLocation(location).build();
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .getNodeIdFromQuery(query).getValue();
    }

    public IpInterfaceDTO getIpInterfaceFromQuery(String tenantId, String ipAddress, String location) {
        Metadata metadata = getMetadata(true, tenantId);

        NodeIdQuery query = NodeIdQuery.newBuilder()
            .setIpAddress(ipAddress).setLocation(location).build();
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .getIpInterfaceFromQuery(query);
    }
}
