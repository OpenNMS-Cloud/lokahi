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

package org.opennms.horizon.inventory.mapper.node;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.DefaultNodeDTO;
import org.opennms.horizon.inventory.mapper.DateTimeMapper;
import org.opennms.horizon.inventory.mapper.EmptyStringMapper;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.inventory.model.node.DefaultNode;
import org.opennms.node.scan.contract.NodeInfoResult;


@Mapper(componentModel = "spring", uses = {EmptyStringMapper.class,
    IpInterfaceMapper.class, SnmpInterfaceMapper.class},
    // Needed for grpc proto mapping
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DefaultNodeMapper extends DateTimeMapper {

    @Mapping(source = "ipInterfacesList", target = "ipInterfaces")
    @Mapping(source = "snmpInterfacesList", target = "snmpInterfaces")
    @Mapping(target = "monitoringLocation", ignore = true)
    DefaultNode dtoToModel(DefaultNodeDTO dto);

    @Mapping(source = "ipInterfaces", target = "ipInterfacesList")
    @Mapping(source = "snmpInterfaces", target = "snmpInterfacesList")
    @Mapping(source = "monitoringLocation.location", target = "monitoringLocation")
    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    DefaultNodeDTO modelToDto(DefaultNode defaultNode);

    @Mapping(target = "objectId", source = "objectId", qualifiedByName = "emptyString")
    @Mapping(target = "systemName", source = "systemName", qualifiedByName = "emptyString")
    @Mapping(target = "systemDescr", source = "systemDescr", qualifiedByName = "emptyString")
    @Mapping(target = "systemLocation", source = "systemLocation", qualifiedByName = "emptyString")
    @Mapping(target = "systemContact", source = "systemContact", qualifiedByName = "emptyString")
    void updateFromNodeInfo(NodeInfoResult nodeInfoResult, @MappingTarget DefaultNode node);

}
