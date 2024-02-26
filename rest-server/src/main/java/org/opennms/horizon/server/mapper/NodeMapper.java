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

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeUpdateDTO;
import org.opennms.horizon.server.model.inventory.Node;
import org.opennms.horizon.server.model.inventory.NodeCreate;
import org.opennms.horizon.server.model.inventory.NodeUpdate;

@Mapper(
        componentModel = "spring",
        uses = {TagMapper.class, IpInterfaceMapper.class, SnmpInterfaceMapper.class, AzureInterfaceMapper.class},
        // Needed for grpc proto mapping
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface NodeMapper {

    @Mapping(source = "ipInterfacesList", target = "ipInterfaces")
    @Mapping(source = "snmpInterfacesList", target = "snmpInterfaces")
    @Mapping(source = "azureInterfacesList", target = "azureInterfaces")
    @Mapping(target = "tags", source = "tagsList", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    Node protoToNode(NodeDTO nodeDTO);

    @Mapping(target = "locationId", source = "locationId", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    NodeCreateDTO nodeCreateToProto(NodeCreate request);

    NodeUpdateDTO nodeUpdateToProto(NodeUpdate request);
}
