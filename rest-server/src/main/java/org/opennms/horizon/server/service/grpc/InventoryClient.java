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
import java.util.stream.Collectors;

import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.shared.constants.GrpcConstants;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InventoryClient {
    private final ManagedChannel channel;
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub locationStub;
    private NodeServiceGrpc.NodeServiceBlockingStub nodeStub;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub systemStub;

    protected void initialStubs() {
        locationStub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
        nodeStub = NodeServiceGrpc.newBlockingStub(channel);
        systemStub = MonitoringSystemServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public NodeDTO createNewNode(NodeCreateDTO node, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).createNode(node);
    }

    public List<NodeDTO> listNodes(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listNodes(Empty.newBuilder().build()).getNodesList();
    }

    public NodeDTO getNodeById(long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getNodeById(Int64Value.of(id));
    }

    public List<MonitoringLocationDTO> listLocations(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listLocations(Empty.newBuilder().build()).getLocationsList();
    }

    public MonitoringLocationDTO getLocationById(long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getLocationById(Int64Value.of(id));
    }

    public List<MonitoringSystemDTO> listMonitoringSystems(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return systemStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listMonitoringSystem(Empty.newBuilder().build()).getSystemsList();
    }

    public MonitoringSystemDTO getSystemBySystemId(String systemId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return systemStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getMonitoringSystemById(StringValue.of(systemId));
    }

    public List<MonitoringLocationDTO> listLocationsByIds(List<DataLoaderFactory.Key> keys) {
        return keys.stream().map(DataLoaderFactory.Key::getToken).findFirst().map(accessToken -> {
            Metadata metadata = new Metadata();
            metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
            List<Int64Value> idValues = keys.stream().map(k->Int64Value.of(k.getId())).collect(Collectors.toList());
            return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listLocationsByIds(IdList.newBuilder().addAllIds(idValues).build()).getLocationsList();
        }).orElseThrow();
    }

    public boolean deleteNode(long nodeId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).deleteNode(Int64Value.of(nodeId)).getValue();
    }

    public boolean deleteMonitoringSystem(String systemId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return systemStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).deleteMonitoringSystem(StringValue.of(systemId)).getValue();
    }
}
