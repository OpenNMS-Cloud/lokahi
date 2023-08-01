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

package org.opennms.horizon.inventory.mapper;


import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.node.scan.contract.SnmpInterfaceResult;

@Mapper(componentModel = "spring", uses = {EmptyStringMapper.class, IpAddressMapper.class})
public interface SnmpInterfaceMapper {
    @Mappings({
        @Mapping(target = "tenantId", source = "tenantId", qualifiedByName = "emptyString"),
        @Mapping(target = "ifDescr", source = "ifDescr", qualifiedByName = "emptyString"),
        @Mapping(target = "ifName", source = "ifName", qualifiedByName = "emptyString"),
        @Mapping(target = "ifAlias", source = "ifAlias", qualifiedByName = "emptyString"),
        @Mapping(target = "physicalAddr", source = "physicalAddr", qualifiedByName = "emptyString")
    })
    SnmpInterface dtoToModel(SnmpInterfaceDTO dto);

    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    SnmpInterfaceDTO modelToDTO(SnmpInterface model);

    @Mappings({
        @Mapping(target = "ifDescr", source = "ifDescr", qualifiedByName = "emptyString"),
        @Mapping(target = "ifName", source = "ifName", qualifiedByName = "emptyString"),
        @Mapping(target = "ifAlias", source = "ifAlias", qualifiedByName = "emptyString"),
        @Mapping(target = "physicalAddr", source = "physicalAddr", qualifiedByName = "emptyString")
    })
    SnmpInterface scanResultToModel(SnmpInterfaceResult result);

    @Mappings({
        @Mapping(target = "ifDescr", source = "ifDescr", qualifiedByName = "emptyString"),
        @Mapping(target = "ifName", source = "ifName", qualifiedByName = "emptyString"),
        @Mapping(target = "ifAlias", source = "ifAlias", qualifiedByName = "emptyString"),
        @Mapping(target = "physicalAddr", source = "physicalAddr", qualifiedByName = "emptyString")
    })
    void updateFromScanResult(SnmpInterfaceResult result, @MappingTarget SnmpInterface snmpInterface);
}
