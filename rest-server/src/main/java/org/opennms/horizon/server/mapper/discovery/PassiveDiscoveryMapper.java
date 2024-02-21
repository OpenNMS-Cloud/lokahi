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
package org.opennms.horizon.server.mapper.discovery;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.server.mapper.TagMapper;
import org.opennms.horizon.server.model.inventory.discovery.passive.PassiveDiscovery;
import org.opennms.horizon.server.model.inventory.discovery.passive.PassiveDiscoveryToggle;
import org.opennms.horizon.server.model.inventory.discovery.passive.PassiveDiscoveryUpsert;

@Mapper(
        componentModel = "spring",
        uses = {TagMapper.class},
        // Needed for grpc proto mapping
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PassiveDiscoveryMapper {

    @Mapping(target = "snmpPorts", source = "portsList")
    @Mapping(target = "snmpCommunities", source = "communitiesList")
    PassiveDiscovery protoToDiscovery(PassiveDiscoveryDTO passiveDiscoveryDTO);

    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    PassiveDiscoveryUpsertDTO discoveryUpsertToProto(PassiveDiscoveryUpsert request);

    PassiveDiscoveryToggleDTO discoveryToggleToProto(PassiveDiscoveryToggle toggle);

    PassiveDiscoveryToggle protoToDiscoveryToggle(PassiveDiscoveryDTO dto);

    default PassiveDiscoveryUpsertDTO discoveryUpsertToProtoCustom(PassiveDiscoveryUpsert request) {
        PassiveDiscoveryUpsertDTO.Builder builder = discoveryUpsertToProto(request).toBuilder();
        builder.addAllPorts(request.getSnmpPorts());
        builder.addAllCommunities(request.getSnmpCommunities());
        return builder.build();
    }
}
