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


import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.PassiveDiscovery;

@Mapper(componentModel = "spring", uses = {},
    // Needed for grpc proto mapping
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PassiveDiscoveryMapper extends DateTimeMapper {

    @Mapping(source = "portsList", target = "snmpPorts")
    @Mapping(source = "communitiesList", target = "snmpCommunities")
    PassiveDiscovery dtoToModel(PassiveDiscoveryCreateDTO dto);


    @Mapping(source = "snmpPorts", target = "portsList")
    @Mapping(source = "createTime", target = "createTimeMsec")
    PassiveDiscoveryDTO modelToDto(PassiveDiscovery model);

    default PassiveDiscoveryDTO modelToDtoCustom(PassiveDiscovery model) {
        PassiveDiscoveryDTO.Builder builder = modelToDto(model).toBuilder();
        builder.addAllCommunities(model.getSnmpCommunities());
        builder.addAllLocations(model.getMonitoringLocations()
            .stream().map(MonitoringLocation::getLocation).toList());
        return builder.build();
    }
}
