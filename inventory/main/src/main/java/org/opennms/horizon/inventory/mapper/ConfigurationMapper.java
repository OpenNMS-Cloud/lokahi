/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.model.Configuration;

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
