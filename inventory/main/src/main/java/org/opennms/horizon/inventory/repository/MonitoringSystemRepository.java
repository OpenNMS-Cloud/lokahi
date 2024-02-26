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
package org.opennms.horizon.inventory.repository;

import java.util.List;
import java.util.Optional;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitoringSystemRepository extends JpaRepository<MonitoringSystem, Long> {
    List<MonitoringSystem> findByTenantId(String tenantId);

    Optional<MonitoringSystem> findBySystemIdAndTenantId(String systemId, String tenantId);

    List<MonitoringSystem> findByMonitoringLocationIdAndTenantId(long locationId, String tenantId);

    Optional<MonitoringSystem> findByMonitoringLocationIdAndSystemIdAndTenantId(
            long locationId, String systemId, String tenantId);

    @Query("SELECT ms " + "FROM MonitoringSystem ms "
            + "WHERE ms.systemId = :systemId "
            + "AND ms.monitoringLocation.location = :locationName "
            + "AND ms.tenantId = :tenantId ")
    Optional<MonitoringSystem> findByMonitoringLocationAndSystemIdAndTenantId(
            @Param("locationName") String locationName,
            @Param("systemId") String systemId,
            @Param("tenantId") String tenantId);

    Optional<MonitoringSystem> findByIdAndTenantId(long id, String tenantId);
}
