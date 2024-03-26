/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import java.util.ArrayList;
import java.util.List;
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

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GraphQLTagService {
    private final InventoryClient client;
    private final TagMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLMutation
    public Mono<List<Tag>> addTagsToNodes(TagListNodesAdd tags, @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        TagCreateListDTO tagCreateListDTO = mapper.tagListAddToProtoCustom(tags);
        TagListDTO tagListDTO = client.addTags(tagCreateListDTO, authHeader);
        return Mono.just(
                tagListDTO.getTagsList().stream().map(mapper::protoToTag).toList());
    }

    @GraphQLMutation
    public Mono<Void> removeTagsFromNodes(TagListNodesRemove tags, @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        TagRemoveListDTO tagRemoveListDTO = mapper.tagListRemoveToProtoCustom(tags);
        client.removeTags(tagRemoveListDTO, authHeader);
        return Mono.empty();
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTagsByNodeId(
            @GraphQLArgument(name = "nodeId") Long nodeId,
            @GraphQLArgument(name = "searchTerm") String searchTerm,
            @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList = client.getTagsByNodeId(nodeId, searchTerm, headerUtil.getAuthHeader(env))
                .getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }

    @GraphQLQuery
    public Mono<List<NodeTags>> getTagsByNodeIds(
            @GraphQLArgument(name = "nodeIds") List<Long> nodeIds, @GraphQLEnvironment ResolutionEnvironment env) {
        List<NodeTags> nodeTags = new ArrayList<>();
        for (Long nodeId : nodeIds) {
            List<TagDTO> tagsDtoList = client.getTagsByNodeId(nodeId, headerUtil.getAuthHeader(env))
                    .getTagsList();
            List<Tag> tagList = tagsDtoList.stream().map(mapper::protoToTag).toList();
            nodeTags.add(new NodeTags(nodeId, tagList));
        }
        return Mono.just(nodeTags);
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTagsByActiveDiscoveryId(
            @GraphQLArgument(name = "activeDiscoveryId") Long activeDiscoveryId,
            @GraphQLArgument(name = "searchTerm") String searchTerm,
            @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList = client.getTagsByActiveDiscoveryId(
                        activeDiscoveryId, searchTerm, headerUtil.getAuthHeader(env))
                .getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTagsByPassiveDiscoveryId(
            @GraphQLArgument(name = "passiveDiscoveryId") Long activeDiscoveryId,
            @GraphQLArgument(name = "searchTerm") String searchTerm,
            @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList = client.getTagsByPassiveDiscoveryId(
                        activeDiscoveryId, searchTerm, headerUtil.getAuthHeader(env))
                .getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }

    @GraphQLQuery
    public Mono<List<Tag>> getTags(
            @GraphQLArgument(name = "searchTerm") String searchTerm, @GraphQLEnvironment ResolutionEnvironment env) {
        List<TagDTO> tagsList =
                client.getTags(searchTerm, headerUtil.getAuthHeader(env)).getTagsList();
        return Mono.just(tagsList.stream().map(mapper::protoToTag).toList());
    }
}
