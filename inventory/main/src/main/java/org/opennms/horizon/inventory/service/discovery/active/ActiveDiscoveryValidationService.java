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
package org.opennms.horizon.inventory.service.discovery.active;

import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.MonitoringLocationService;

import java.util.Objects;

public interface ActiveDiscoveryValidationService {

    ActiveDiscoveryRepository getActiveDiscoveryRepository();
    MonitoringLocationService getMonitoringLocationService();

    default void validateActiveDiscoveryName(String name, String tenantId) {
        validateActiveDiscoveryName(name, -1 , tenantId);
    }

    default void validateActiveDiscoveryName(String name, long discoveryId, String tenantId) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(tenantId);
        if (StringUtils.isBlank(name)) {
            throw new InventoryRuntimeException("Blank discovery name");
        }
        if (getActiveDiscoveryRepository().findByNameAndTenantId(name.trim(), tenantId)
            .stream().anyMatch(d -> d.getId() != discoveryId)) {
            throw new InventoryRuntimeException("Duplicate active discovery with name " + name);
        }
    }

    default void validateLocation(String locationId, String tenantId) {
        if (getMonitoringLocationService().findByLocationIdAndTenantId(Long.parseLong(locationId), tenantId).isEmpty()) {
            throw new LocationNotFoundException("Location not found with id " + locationId);
        }
    }
}
