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
import org.opennms.horizon.inventory.dto.AddressCreateDTO;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.dto.AddressUpdateDTO;
import org.opennms.horizon.server.model.inventory.Address;
import org.opennms.horizon.server.model.inventory.AddressCreate;
import org.opennms.horizon.server.model.inventory.AddressUpdate;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "addressLine1", source = "addressLine1", defaultValue = "")
    @Mapping(target = "addressLine2", source = "addressLine2", defaultValue = "")
    @Mapping(target = "postalCode", source = "postalCode", defaultValue = "")
    @Mapping(target = "city", source = "city", defaultValue = "")
    @Mapping(target = "state", source = "state", defaultValue = "")
    @Mapping(target = "country", source = "country", defaultValue = "")
    AddressDTO addressToProto(Address address);

    @Mapping(target = "addressLine1", source = "addressLine1", defaultValue = "")
    @Mapping(target = "addressLine2", source = "addressLine2", defaultValue = "")
    @Mapping(target = "postalCode", source = "postalCode", defaultValue = "")
    @Mapping(target = "city", source = "city", defaultValue = "")
    @Mapping(target = "state", source = "state", defaultValue = "")
    @Mapping(target = "country", source = "country", defaultValue = "")
    Address protoToAddress(AddressDTO address);

    @Mapping(target = "addressLine1", source = "addressLine1", defaultValue = "")
    @Mapping(target = "addressLine2", source = "addressLine2", defaultValue = "")
    @Mapping(target = "postalCode", source = "postalCode", defaultValue = "")
    @Mapping(target = "city", source = "city", defaultValue = "")
    @Mapping(target = "state", source = "state", defaultValue = "")
    @Mapping(target = "country", source = "country", defaultValue = "")
    AddressCreateDTO addressCreateToAddressCreateProto(AddressCreate address);

    @Mapping(target = "addressLine1", source = "addressLine1", defaultValue = "")
    @Mapping(target = "addressLine2", source = "addressLine2", defaultValue = "")
    @Mapping(target = "postalCode", source = "postalCode", defaultValue = "")
    @Mapping(target = "city", source = "city", defaultValue = "")
    @Mapping(target = "state", source = "state", defaultValue = "")
    @Mapping(target = "country", source = "country", defaultValue = "")
    AddressUpdateDTO addressUpdateToAddressUpdateProto(AddressUpdate address);
}
