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
import java.util.concurrent.TimeUnit;

import org.opennms.horizon.inventory.discovery.DiscoveryConfigDTO;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigOperationGrpc;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigRequest;
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialServiceGrpc;
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdList;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.util.StringUtils;

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
    private final long deadline;
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub locationStub;
    private NodeServiceGrpc.NodeServiceBlockingStub nodeStub;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub systemStub;
    private AzureCredentialServiceGrpc.AzureCredentialServiceBlockingStub azureCredentialStub;
    private TagServiceGrpc.TagServiceBlockingStub tagStub;
    private DiscoveryConfigOperationGrpc.DiscoveryConfigOperationBlockingStub discoveryConfigStub;

    protected void initialStubs() {
        locationStub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
        nodeStub = NodeServiceGrpc.newBlockingStub(channel);
        systemStub = MonitoringSystemServiceGrpc.newBlockingStub(channel);
        azureCredentialStub = AzureCredentialServiceGrpc.newBlockingStub(channel);
        tagStub = TagServiceGrpc.newBlockingStub(channel);
        discoveryConfigStub = DiscoveryConfigOperationGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public List<DiscoveryConfigDTO> createDiscoveryConfig(DiscoveryConfigRequest request, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return discoveryConfigStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .createConfig(request).getDiscoverConfigsList();
    }

    public List<DiscoveryConfigDTO> listDiscoveryConfig(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return discoveryConfigStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .listDiscoveryConfig(Empty.getDefaultInstance()).getDiscoverConfigsList();
    }

    public DiscoveryConfigDTO getDiscoveryConfigByName(String name, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return discoveryConfigStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .getDiscoveryConfigByName(StringValue.of(name));
    }

    public NodeDTO createNewNode(NodeCreateDTO node, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).createNode(node);
    }

    public List<NodeDTO> listNodes(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listNodes(Empty.newBuilder().build()).getNodesList();
    }

    public NodeDTO getNodeById(long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getNodeById(Int64Value.of(id));
    }

    public List<MonitoringLocationDTO> listLocations(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listLocations(Empty.newBuilder().build()).getLocationsList();
    }

    public MonitoringLocationDTO getLocationById(long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getLocationById(Int64Value.of(id));
    }

    public List<MonitoringSystemDTO> listMonitoringSystems(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return systemStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listMonitoringSystem(Empty.newBuilder().build()).getSystemsList();
    }

    public MonitoringSystemDTO getSystemBySystemId(String systemId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return systemStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getMonitoringSystemById(StringValue.of(systemId));
    }

    public List<MonitoringLocationDTO> listLocationsByIds(List<DataLoaderFactory.Key> keys) {
        return keys.stream().map(DataLoaderFactory.Key::getToken).findFirst().map(accessToken -> {
            Metadata metadata = new Metadata();
            metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
            List<Int64Value> idValues = keys.stream().map(k->Int64Value.of(k.getId())).toList();
            return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listLocationsByIds(IdList.newBuilder().addAllIds(idValues).build()).getLocationsList();
        }).orElseThrow();
    }

    public List<MonitoringLocationDTO> searchLocations(String searchTerm, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .searchLocations(StringValue.of(searchTerm)).getLocationsList();
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

    public boolean startScanByNodeIds(List<Long> ids, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).startNodeScanByIds(NodeIdList.newBuilder().addAllIds(ids).build()).getValue();
    }

    public AzureCredentialDTO createNewAzureCredential(AzureCredentialCreateDTO azureCredential, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return azureCredentialStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).createCredentials(azureCredential);
    }

    public TagListDTO addTags(TagCreateListDTO tagCreateList, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).addTags(tagCreateList);
    }

    public void removeTags(TagRemoveListDTO tagRemoveList, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).removeTags(tagRemoveList);
    }

    public TagListDTO getTagsByNodeId(long nodeId, String searchTerm, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setNodeId(nodeId)
            .setParams(buildTagListParams(searchTerm))
            .build();
        return tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getTagsByEntityId(params);
    }

    public TagListDTO getTagsByAzureCredentialId(long azureCredentialId, String searchTerm, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setAzureCredentialId(azureCredentialId)
            .setParams(buildTagListParams(searchTerm))
            .build();
        return tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getTagsByEntityId(params);
    }

    public TagListDTO getTags(String searchTerm, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ListAllTagsParamsDTO params = ListAllTagsParamsDTO.newBuilder()
            .setParams(buildTagListParams(searchTerm))
            .build();
        return tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getTags(params);
    }

    private TagListParamsDTO buildTagListParams(String searchTerm) {
        TagListParamsDTO.Builder paramBuilder = TagListParamsDTO.newBuilder();
        if (StringUtils.hasText(searchTerm)) {
            paramBuilder.setSearchTerm(searchTerm);
        }
        return paramBuilder.build();
    }
}
