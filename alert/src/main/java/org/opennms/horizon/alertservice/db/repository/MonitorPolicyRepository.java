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
package org.opennms.horizon.alertservice.db.repository;

import java.util.List;
import java.util.Optional;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MonitorPolicyRepository extends JpaRepository<MonitorPolicy, Long> {
    List<MonitorPolicy> findAllByTenantId(String tenantId);

    Optional<MonitorPolicy> findByIdAndTenantId(Long id, String tenantId);

    Optional<MonitorPolicy> findByNameAndTenantId(String name, String tenantId);

    void deleteByIdAndTenantId(Long id, String tenantId);

    @Query(
            "SELECT policy FROM AlertCondition ac INNER JOIN ac.rule as pr INNER JOIN pr.policy as policy WHERE ac.id = ?1")
    Optional<MonitorPolicy> findMonitoringPolicyByAlertConditionId(Long alertConditionId);
}
