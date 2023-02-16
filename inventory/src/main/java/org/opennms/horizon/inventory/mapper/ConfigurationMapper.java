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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.model.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Mapper(componentModel = "spring")
public interface ConfigurationMapper {
    Configuration dtoToModel(ConfigurationDTO dto);
    ConfigurationDTO modelToDTO(Configuration model);

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "tenantId", ignore = true),
        @Mapping(target = "location", ignore = true),
        @Mapping(target = "key", ignore = true)
    })
    void updateFromDTO(ConfigurationDTO configDTO, @MappingTarget Configuration configuration);

    default JsonNode map(String value) throws JsonProcessingException {
        return new ObjectMapper().readTree(value);
    }

    default String map(JsonNode value) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(value);
    }
}
