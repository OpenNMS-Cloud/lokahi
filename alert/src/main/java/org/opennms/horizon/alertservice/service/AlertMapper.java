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

package org.opennms.horizon.alertservice.service;

import java.util.Date;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;
import org.opennms.horizon.alertservice.db.entity.Alert;

@Mapper(componentModel = "spring",
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AlertMapper {

    AlertMapper INSTANCE = Mappers.getMapper( AlertMapper.class );

    @Mappings({
        @Mapping(target = "databaseId", source = "id"),
        @Mapping(target = "lastUpdateTimeMs", source = "lastEventTime"),
        @Mapping(target = "isAcknowledged", expression = "java(alert.getAcknowledgedByUser() != null ? true : false)"),
        @Mapping(target = "ackUser", source = "acknowledgedByUser"),
        @Mapping(target = "ackTimeMs", source = "acknowledgedAt"),
        @Mapping(target = "monitoringPolicyIdList", source = "monitoringPolicyId")
    })
    org.opennms.horizon.alerts.proto.Alert toProto(Alert alert);

    default long mapDateToLongMs(Date value) {
        return value == null ? 0L : value.getTime();
    }
}
