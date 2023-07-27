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
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.AzureInterfaceDTO;
import org.opennms.horizon.inventory.model.AzureInterface;

@Mapper(componentModel = "spring", uses = {EmptyStringMapper.class, IpAddressMapper.class})
public interface AzureInterfaceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicIpAddress", source = "publicIpAddress.ipAddress", qualifiedByName = "emptyString")
    @Mapping(target = "privateIpId", source = "name", qualifiedByName = "emptyString")
    @Mapping(target = "publicIpId", source = "publicIpAddress.name", qualifiedByName = "emptyString")
    @Mapping(target = "interfaceName", source = "interfaceName", qualifiedByName = "emptyString")
    @Mapping(target = "location", source = "location", qualifiedByName = "emptyString")
    AzureInterface scanResultToModel(AzureScanNetworkInterfaceItem scanResult);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicIpAddress", source = "publicIpAddress.ipAddress", qualifiedByName = "emptyString")
    @Mapping(target = "privateIpId", source = "name", qualifiedByName = "emptyString")
    @Mapping(target = "publicIpId", source = "publicIpAddress.name", qualifiedByName = "emptyString")
    @Mapping(target = "interfaceName", source = "interfaceName", qualifiedByName = "emptyString")
    void updateFromScanResult(@MappingTarget AzureInterface result, AzureScanNetworkInterfaceItem scanResult);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    AzureInterfaceDTO modelToDTO(AzureInterface model);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    AzureInterface dtoToModel(AzureInterfaceDTO model);
}
