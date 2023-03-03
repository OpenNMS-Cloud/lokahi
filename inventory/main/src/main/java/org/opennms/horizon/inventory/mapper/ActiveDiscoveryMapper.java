/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryRequest;
import org.opennms.horizon.inventory.model.ActiveDiscoveryConfig;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ActiveDiscoveryMapper {

    @Mapping(target = "name", source = "configName")
    @Mapping(target = "ipAddressEntries", source = "ipAddressesList")
    @Mapping(target = "snmpCommunityStrings", source = "snmpConf.readCommunityList",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "snmpPorts", source = "snmpConf.portsList",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    ActiveDiscoveryConfig mapRequest(ActiveDiscoveryRequest discoveryConfigRequest);

    @Mapping(target = "configName", source = "name")
    @Mapping(target = "ipAddressesList", source = "ipAddressEntries")
    @Mapping(target = "snmpConf.readCommunityList", source = "snmpCommunityStrings",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "snmpConf.portsList", source = "snmpPorts",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    ActiveDiscoveryDTO modelToDto(ActiveDiscoveryConfig activeDiscoveryConfig);

    @Mapping(target = "name", source = "configName")
    @Mapping(target = "ipAddressEntries", source = "ipAddressesList")
    @Mapping(target = "snmpCommunityStrings", source = "snmpConf.readCommunityList",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "snmpPorts", source = "snmpConf.portsList",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    ActiveDiscoveryConfig dtoToModel(ActiveDiscoveryDTO discoveryConfigDTO);

}
