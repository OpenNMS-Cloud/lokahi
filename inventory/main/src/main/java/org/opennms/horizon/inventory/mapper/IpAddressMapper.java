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

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.opennms.horizon.shared.utils.InetAddressUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
@Mapper(componentModel = "spring")
public interface IpAddressMapper {
    default InetAddress map(String value) throws UnknownHostException {
        if(StringUtils.isNotEmpty(value)) {
            return InetAddressUtils.getInetAddress(value);
        } else {
            return null;
        }
    }

    default String map(InetAddress value) {
        if (value != null) {
            return InetAddressUtils.toIpAddrString(value);
        } else {
            return null;
        }
    }
}
