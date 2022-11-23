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

package org.opennms.horizon.server.service.grpc;

import java.util.List;

import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class InventoryClient {
    private final ManagedChannel channel;
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub locationStub;
    private NodeServiceGrpc.NodeServiceBlockingStub nodeStub;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub systemStub;

    //TODO: hardcoded tenantId will be removed in HS-598
    private final String tenantId = "4ab6020d-6ee8-4087-afa4-114604fe21e4";

    public InventoryClient(String serverAddress) {
        channel = ManagedChannelBuilder.forTarget(serverAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
        initialStubs();
    }

    private void initialStubs() {
        locationStub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
        nodeStub = NodeServiceGrpc.newBlockingStub(channel);
        systemStub = MonitoringSystemServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if(channel!=null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    //TODO: add error handling
    public NodeDTO createNewNode(NodeCreateDTO node) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).createNode(node);
    }

    public List<NodeDTO> listNodes() {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listNodes(Empty.newBuilder().build()).getNodesList();
    }

    public NodeDTO getNodeById(long id) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getNodeById(Int64Value.of(id));
    }

    public List<MonitoringLocationDTO> listLocations() {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listLocations(Empty.newBuilder().build()).getLocationsList();
    }

    public MonitoringLocationDTO getLocationById(long id) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getLocationById(Int64Value.of(id));
    }

    public List<MonitoringSystemDTO> listMonitoringSystems() {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        return systemStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listMonitoringSystem(Empty.newBuilder().build()).getSystemsList();
    }

    public MonitoringSystemDTO getSystemBySystemId(String systemId) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        return systemStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getMonitoringSystemById(StringValue.of(systemId));
    }

}
