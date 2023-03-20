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

package org.opennms.horizon.server.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.SyntheticTestCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestDTO;
import org.opennms.horizon.server.model.inventory.st.CreateSyntheticTransactionTest;
import org.opennms.horizon.server.model.inventory.st.SyntheticTransactionTest;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, uses = SyntheticTransactionPluginConfigurationMapper.class
)
public interface SyntheticTransactionTestMapper {

    @Mappings({
        @Mapping(source = "id.tenant", target = "tenantId"),
        @Mapping(source = "id.id", target = "id"),
        @Mapping(source = "locationsList", target = "locations")
    })
    SyntheticTransactionTest fromProtobuf(SyntheticTestDTO proto);

    @Mappings({
        @Mapping(source = "syntheticTransactionId", target = "syntheticTransactionId.id"),
        @Mapping(source = "locations", target = "locationsList")
    })
    SyntheticTestCreateDTO createRequest(CreateSyntheticTransactionTest test);

}
