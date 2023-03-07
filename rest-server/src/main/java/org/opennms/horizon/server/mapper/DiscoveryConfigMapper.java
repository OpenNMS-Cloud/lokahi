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

package org.opennms.horizon.server.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryRequest;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.server.model.inventory.discovery.ActiveDiscovery;
import org.opennms.horizon.server.model.inventory.discovery.CreateDiscoveryConfigRequest;
import org.opennms.horizon.server.model.inventory.discovery.SNMPConfig;

import java.util.List;

@Mapper(componentModel = "spring",
    uses = {TagMapper.class}, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface DiscoveryConfigMapper {
    @Mappings({
        @Mapping(source = "readCommunityList", target = "readCommunities"),
        @Mapping(source = "portsList", target = "ports")
    })
    SNMPConfig snmpDtoToModel(SNMPConfigDTO snmpDto);

    @Mappings({
        @Mapping(source = "readCommunities", target = "readCommunityList"),
        @Mapping(source = "ports", target = "portsList")
    })
    SNMPConfigDTO snmpConfigToDTO(SNMPConfig snmpConfig);

    @Mappings({
        @Mapping(target = "ipAddressesList", source = "ipAddresses"),
        @Mapping(target = "snmpConf", source = "snmpConfig", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS),
        @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    })
    ActiveDiscoveryRequest mapRequest(CreateDiscoveryConfigRequest request);

    @Mappings({
        @Mapping(source = "ipAddressesList", target = "ipAddresses"),
        @Mapping(source = "snmpConf", target = "snmpConfig")
    })
    ActiveDiscovery configDtoToModel(ActiveDiscoveryDTO configDTO);

    List<ActiveDiscovery> configDtoListToConfig(List<ActiveDiscoveryDTO> dtoList);
}
