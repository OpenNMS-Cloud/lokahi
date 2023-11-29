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

package org.opennms.horizon.server.mapper.alert;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.server.model.alerts.AlertCount;

@Mapper(componentModel = "spring", uses = AlertCountTypeMapper.class, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AlertsCountMapper {
    public AlertCount protoToAlertCount(org.opennms.horizon.alerts.proto.AlertCount alertCount);
}
