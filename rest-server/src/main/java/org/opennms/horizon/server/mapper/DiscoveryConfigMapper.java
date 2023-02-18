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

import java.util.List;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigDTO;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigRequest;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.server.model.inventory.discovery.CreateDiscoveryConfigRequest;
import org.opennms.horizon.server.model.inventory.discovery.DiscoveryConfig;
import org.opennms.horizon.server.model.inventory.discovery.SNMPConfig;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
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
        @Mapping(target = "snmpConf", source = "snmpConfig")
    })
     DiscoveryConfigRequest mapRequest(CreateDiscoveryConfigRequest request);

    @Mappings({
        @Mapping(source = "ipAddressesList", target = "ipAddresses"),
        @Mapping(source = "snmpConf", target = "snmpConfig")
    })
    DiscoveryConfig configDtoToModel(DiscoveryConfigDTO configDTO);

    List<DiscoveryConfig> configDtoListToConfig(List<DiscoveryConfigDTO> dtoList);
}
