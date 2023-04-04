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

package org.opennms.horizon.server.mapper.node;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.DefaultNodeCreateDTO;
import org.opennms.horizon.inventory.dto.DefaultNodeDTO;
import org.opennms.horizon.server.mapper.IpInterfaceMapper;
import org.opennms.horizon.server.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.server.mapper.TagMapper;
import org.opennms.horizon.server.model.inventory.NodeCreate;
import org.opennms.horizon.server.model.inventory.node.DefaultNode;


@Mapper(componentModel = "spring", uses = {TagMapper.class, IpInterfaceMapper.class, SnmpInterfaceMapper.class},
    // Needed for grpc proto mapping
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface DefaultNodeMapper {

    @Mapping(source = "ipInterfacesList", target = "ipInterfaces")
    @Mapping(source = "snmpInterfacesList", target = "snmpInterfaces")
    DefaultNode protoToNode(DefaultNodeDTO dto);

    @Mapping(target = "location", source = "location", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    DefaultNodeCreateDTO nodeCreateToProto(NodeCreate request);
}
