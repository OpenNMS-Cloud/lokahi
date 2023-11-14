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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.opennms.horizon.server.model.inventory.SnmpInterface;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.server.model.inventory.SnmpInterfaceAdminStatus;
import org.opennms.horizon.server.model.inventory.SnmpInterfaceOperatorStatus;

@Mapper(componentModel = "spring")
public interface SnmpInterfaceMapper {
    @Mapping(source = "ifAdminStatus", target = "ifAdminStatus", qualifiedByName = "mapAdminStatus")
    @Mapping(source = "ifOperatorStatus", target = "ifOperatorStatus", qualifiedByName = "mapOperatorStatus")
    SnmpInterface protobufToSnmpInterface(SnmpInterfaceDTO protoBuf);

    @Named("mapAdminStatus")
    default String mapAdminStatus(int ifAdminStatus) {
        var status = SnmpInterfaceAdminStatus.valueOf(ifAdminStatus);
        return status != null ? status.name() : null;
    }

    @Named("mapOperatorStatus")
    default String mapOperatorStatus(int ifOperatorStatus) {
        var status = SnmpInterfaceOperatorStatus.valueOf(ifOperatorStatus);
        return status != null ? status.name() : null;
    }
}
