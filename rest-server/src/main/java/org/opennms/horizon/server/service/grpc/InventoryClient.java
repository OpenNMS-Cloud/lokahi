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


import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.MonitoredStateQuery;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdList;
import org.opennms.horizon.inventory.dto.NodeLabelSearchQuery;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryListDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagNameQuery;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class InventoryClient {
    private final ManagedChannel channel;
    private final long deadline;
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub locationStub;
    private NodeServiceGrpc.NodeServiceBlockingStub nodeStub;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub systemStub;
    private TagServiceGrpc.TagServiceBlockingStub tagStub;
    private ActiveDiscoveryServiceGrpc.ActiveDiscoveryServiceBlockingStub activeDiscoveryServiceBlockingStub;
    private IcmpActiveDiscoveryServiceGrpc.IcmpActiveDiscoveryServiceBlockingStub icmpActiveDiscoveryServiceBlockingStub;
    private AzureActiveDiscoveryServiceGrpc.AzureActiveDiscoveryServiceBlockingStub azureActiveDiscoveryServiceBlockingStub;
    private PassiveDiscoveryServiceGrpc.PassiveDiscoveryServiceBlockingStub passiveDiscoveryServiceBlockingStub;

    protected void initialStubs() {
        locationStub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
        nodeStub = NodeServiceGrpc.newBlockingStub(channel);
        systemStub = MonitoringSystemServiceGrpc.newBlockingStub(channel);

        tagStub = TagServiceGrpc.newBlockingStub(channel);
        activeDiscoveryServiceBlockingStub = ActiveDiscoveryServiceGrpc.newBlockingStub(channel);
        icmpActiveDiscoveryServiceBlockingStub = IcmpActiveDiscoveryServiceGrpc.newBlockingStub(channel);
        azureActiveDiscoveryServiceBlockingStub = AzureActiveDiscoveryServiceGrpc.newBlockingStub(channel);
        passiveDiscoveryServiceBlockingStub = PassiveDiscoveryServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public List<ActiveDiscoveryDTO> listActiveDiscoveries(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return activeDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .listDiscoveries(Empty.getDefaultInstance()).getActiveDiscoveriesList();
    }

    public IcmpActiveDiscoveryDTO createIcmpActiveDiscovery(IcmpActiveDiscoveryCreateDTO request, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return icmpActiveDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .createDiscovery(request);
    }

    public List<IcmpActiveDiscoveryDTO> listIcmpDiscoveries(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return icmpActiveDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .listDiscoveries(Empty.getDefaultInstance()).getDiscoveriesList();
    }

    public IcmpActiveDiscoveryDTO getIcmpDiscoveryById(Long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return icmpActiveDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .getDiscoveryById(Int64Value.of(id));
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

    public List<NodeDTO> listNodesByMonitoredState(String monitoredState, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        MonitoredStateQuery query = MonitoredStateQuery.newBuilder().setMonitoredState(MonitoredState.valueOf(monitoredState)).build();
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listNodesByMonitoredState(query).getNodesList();
    }

    public List<NodeDTO> listNodesByNodeLabelSearch(String labelSearchTerm, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        NodeLabelSearchQuery query = NodeLabelSearchQuery.newBuilder().setSearchTerm(labelSearchTerm).build();
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listNodesByNodeLabel(query).getNodesList();
    }

    public List<NodeDTO> listNodesByTags(List<String> tags, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        TagNameQuery query = TagNameQuery.newBuilder().addAllTags(tags).build();
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listNodesByTags(query).getNodesList();
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
            List<Int64Value> idValues = keys.stream().map(k -> Int64Value.of(k.getId())).toList();
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

    public AzureActiveDiscoveryDTO createAzureActiveDiscovery(AzureActiveDiscoveryCreateDTO request, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return azureActiveDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).createDiscovery(request);
    }

    public PassiveDiscoveryDTO upsertPassiveDiscovery(PassiveDiscoveryUpsertDTO passiveDiscovery, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return passiveDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).upsertDiscovery(passiveDiscovery);
    }

    public PassiveDiscoveryListDTO listPassiveDiscoveries(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return passiveDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listAllDiscoveries(Empty.getDefaultInstance());
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
            .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(nodeId))
            .setParams(buildTagListParams(searchTerm))
            .build();
        return tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getTagsByEntityId(params);
    }

    public TagListDTO getTagsByNodeId(long nodeId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(nodeId))
            .build();
        return tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getTagsByEntityId(params);
    }

    public TagListDTO getTagsByActiveDiscoveryId(long activeDiscoveryId, String searchTerm, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setActiveDiscoveryId(activeDiscoveryId))
            .setParams(buildTagListParams(searchTerm))
            .build();
        return tagStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getTagsByEntityId(params);
    }

    public TagListDTO getTagsByPassiveDiscoveryId(long passiveDiscoveryId, String searchTerm, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(passiveDiscoveryId))
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

    public PassiveDiscoveryDTO createPassiveDiscoveryToggle(PassiveDiscoveryToggleDTO request, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return passiveDiscoveryServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).toggleDiscovery(request);
    }

    public IpInterfaceDTO getIpInterfaceById(long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return nodeStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getIpInterfaceById(Int64Value.of(id));
    }

    public MonitoringLocationDTO createLocation(MonitoringLocationDTO location, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).createLocation(location);
    }

    public MonitoringLocationDTO updateLocation(MonitoringLocationDTO location, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).updateLocation(location);
    }

    public boolean deleteLocation(long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return locationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).deleteLocation(Int64Value.of(id)).getValue();
    }
}
