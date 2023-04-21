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

package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.server.mapper.TagMapper;
import org.opennms.horizon.server.model.inventory.tag.NodeTags;
import org.opennms.horizon.server.model.inventory.tag.Tag;
import org.opennms.horizon.server.model.inventory.tag.TagListNodesAdd;
import org.opennms.horizon.server.model.inventory.tag.TagListNodesRemove;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcTagService {
    private final InventoryClient client;
    private final TagMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLMutation
    public Mono<List<Tag>> addTagsToNodes(TagListNodesAdd tags, @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        TagCreateListDTO tagCreateListDTO = mapper.tagListAddToProtoCustom(tags);
        TagListDTO tagListDTO = client.addTags(tagCreateListDTO, authHeader);
        return Mono.just(tagListDTO.getTagsList().stream().map(mapper::protoToTag).toList());
    }

    @GraphQLMutation
    public Mono<Void> removeTagsFromNodes(TagListNodesRemove tags, @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        TagRemoveListDTO tagRemoveListDTO = mapper.tagListRemoveToProtoCustom(tags);
        client.removeTags(tagRemoveListDTO, authHeader);
        return Mono.empty();
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTagsByNodeId(@GraphQLArgument(name = "nodeId") Long nodeId,
                                           @GraphQLArgument(name = "searchTerm") String searchTerm,
                                           @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList = client.getTagsByNodeId(nodeId, searchTerm, headerUtil.getAuthHeader(env)).getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }

    @GraphQLQuery
    public Mono<List<NodeTags>> getTagsByNodeIds(@GraphQLArgument(name = "nodeIds") List<Long> nodeIds,
                                                 @GraphQLEnvironment ResolutionEnvironment env) {
        List<NodeTags> nodeTags = new ArrayList<>();
        for (Long nodeId : nodeIds) {
            List<TagDTO> tagsDtoList = client.getTagsByNodeId(nodeId, headerUtil.getAuthHeader(env)).getTagsList();
            List<Tag> tagList = tagsDtoList.stream().map(mapper::protoToTag).toList();
            nodeTags.add(new NodeTags(nodeId, tagList));
        }
        return Mono.just(nodeTags);
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTagsByActiveDiscoveryId(@GraphQLArgument(name = "activeDiscoveryId") Long activeDiscoveryId,
                                                      @GraphQLArgument(name = "searchTerm") String searchTerm,
                                                      @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList = client.getTagsByActiveDiscoveryId(activeDiscoveryId, searchTerm, headerUtil.getAuthHeader(env)).getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTagsByPassiveDiscoveryId(@GraphQLArgument(name = "passiveDiscoveryId") Long activeDiscoveryId,
                                                      @GraphQLArgument(name = "searchTerm") String searchTerm,
                                                      @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList = client.getTagsByPassiveDiscoveryId(activeDiscoveryId, searchTerm, headerUtil.getAuthHeader(env)).getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTags(@GraphQLArgument(name = "searchTerm") String searchTerm,
                                   @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList = client.getTags(searchTerm, headerUtil.getAuthHeader(env)).getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }
}
