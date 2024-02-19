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
package org.opennms.horizon.inventory.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.node.scan.contract.NodeInfoResult;

@Mapper(
        componentModel = "spring",
        uses = {EmptyStringMapper.class, IpInterfaceMapper.class, SnmpInterfaceMapper.class, AzureInterfaceMapper.class
        },
        // Needed for grpc proto mapping
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface NodeMapper extends DateTimeMapper {

    @Mappings({
        @Mapping(source = "ipInterfacesList", target = "ipInterfaces"),
        @Mapping(source = "snmpInterfacesList", target = "snmpInterfaces"),
        @Mapping(source = "azureInterfacesList", target = "azureInterfaces"),
        @Mapping(source = "tagsList", target = "tags")
    })
    Node dtoToModel(NodeDTO dto);

    @Mappings({
        @Mapping(source = "ipInterfaces", target = "ipInterfacesList"),
        @Mapping(source = "snmpInterfaces", target = "snmpInterfacesList"),
        @Mapping(source = "azureInterfaces", target = "azureInterfacesList"),
        @Mapping(source = "tags", target = "tagsList"),
        @Mapping(source = "monitoringLocation.location", target = "location")
    })
    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    NodeDTO modelToDTO(Node model);

    @Mappings({
        @Mapping(target = "objectId", source = "objectId", qualifiedByName = "emptyString"),
        @Mapping(target = "systemName", source = "systemName", qualifiedByName = "emptyString"),
        @Mapping(target = "systemDescr", source = "systemDescr", qualifiedByName = "emptyString"),
        @Mapping(target = "systemLocation", source = "systemLocation", qualifiedByName = "emptyString"),
        @Mapping(target = "systemContact", source = "systemContact", qualifiedByName = "emptyString"),
    })
    void updateFromNodeInfo(NodeInfoResult nodeInfoResult, @MappingTarget Node node);
}
