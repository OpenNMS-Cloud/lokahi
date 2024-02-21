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
package org.opennms.horizon.server.mapper;

import com.google.protobuf.Int64Value;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.server.model.inventory.tag.Tag;
import org.opennms.horizon.server.model.inventory.tag.TagCreate;
import org.opennms.horizon.server.model.inventory.tag.TagListMonitorPolicyAdd;
import org.opennms.horizon.server.model.inventory.tag.TagListNodesAdd;
import org.opennms.horizon.server.model.inventory.tag.TagListNodesRemove;

@Mapper(
        componentModel = "spring",
        uses = {},
        // Needed for grpc proto mapping
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface TagMapper {
    Tag protoToTag(TagDTO tagDTO);

    default TagCreateListDTO tagListAddToProtoCustom(TagListNodesAdd tags) {
        TagCreateListDTO.Builder builder = tagListAddToProto(tags).toBuilder();
        builder.addAllEntityIds(tags.getNodeIds().stream()
                .map(value -> TagEntityIdDTO.newBuilder().setNodeId(value).build())
                .toList());
        return builder.build();
    }

    default TagCreateListDTO tagListAddToProtoCustom(TagListMonitorPolicyAdd tags) {
        TagCreateListDTO.Builder builder = tagListAddToProto(tags).toBuilder();
        builder.addEntityIds(TagEntityIdDTO.newBuilder()
                .setMonitoringPolicyId(tags.getMonitorPolicyId())
                .build());
        return builder.build();
    }

    default TagRemoveListDTO tagListRemoveToProtoCustom(TagListNodesRemove tags) {
        TagRemoveListDTO.Builder builder = tagListRemoveToProto(tags).toBuilder();
        builder.addAllEntityIds(tags.getNodeIds().stream()
                .map(value -> TagEntityIdDTO.newBuilder().setNodeId(value).build())
                .toList());
        return builder.build();
    }

    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    TagCreateListDTO tagListAddToProto(TagListNodesAdd tags);

    @Mapping(target = "tagIdsList", source = "tagIds", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    TagRemoveListDTO tagListRemoveToProto(TagListNodesRemove tags);

    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    TagCreateListDTO tagListAddToProto(TagListMonitorPolicyAdd tags);

    TagCreateDTO tagCreateToProto(TagCreate tagCreate);

    default Int64Value longToInt64Value(Long value) {
        return Int64Value.of(value);
    }
}
