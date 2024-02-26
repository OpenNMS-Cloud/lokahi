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
package org.opennms.horizon.inventory.service.discovery.active;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.MonitoringLocationService;

public interface ActiveDiscoveryValidationService {

    ActiveDiscoveryRepository getActiveDiscoveryRepository();

    MonitoringLocationService getMonitoringLocationService();

    default void validateActiveDiscoveryName(String name, String tenantId) {
        validateActiveDiscoveryName(name, -1, tenantId);
    }

    default void validateActiveDiscoveryName(String name, long discoveryId, String tenantId) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(tenantId);
        if (StringUtils.isBlank(name)) {
            throw new InventoryRuntimeException("Blank discovery name");
        }
        if (getActiveDiscoveryRepository().findByNameAndTenantId(name.trim(), tenantId).stream()
                .anyMatch(d -> d.getId() != discoveryId)) {
            throw new InventoryRuntimeException("Duplicate active discovery with name " + name);
        }
    }

    default void validateLocation(String locationId, String tenantId) {
        if (getMonitoringLocationService()
                .findByLocationIdAndTenantId(Long.parseLong(locationId), tenantId)
                .isEmpty()) {
            throw new LocationNotFoundException("Location not found with id " + locationId);
        }
    }
}
