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

package org.opennms.horizon.server.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.inventory.Node;
import org.opennms.horizon.server.model.inventory.NodeCreate;


@Mapper(componentModel = "spring", uses = {TagMapper.class, IpInterfaceMapper.class, SnmpInterfaceMapper.class,
    AzureInterfaceMapper.class},
    // Needed for grpc proto mapping
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface NodeMapper {

    @Mapping(source = "ipInterfacesList", target = "ipInterfaces")
    @Mapping(source = "snmpInterfacesList", target = "snmpInterfaces")
    @Mapping(source = "azureInterfacesList", target = "azureInterfaces")
    Node protoToNode(NodeDTO nodeDTO);

    @Mapping(target = "locationId", source = "locationId", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    NodeCreateDTO nodeCreateToProto(NodeCreate request);
}
