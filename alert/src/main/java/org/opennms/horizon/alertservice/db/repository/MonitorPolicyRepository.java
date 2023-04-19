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

package org.opennms.horizon.alertservice.db.repository;

import java.util.List;
import java.util.Optional;

import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MonitorPolicyRepository extends JpaRepository<MonitorPolicy, Long> {
    List<MonitorPolicy> findAllByTenantId(String tenantId);
    Optional<MonitorPolicy> findByIdAndTenantId(Long id, String tenantId);
    Optional<MonitorPolicy> findByName(String name);

    @Query("SELECT policy FROM TriggerEvent te INNER JOIN te.rule as pr INNER JOIN pr.policy as policy WHERE te.id = ?1")
    Optional<MonitorPolicy> findMonitoringPolicyByTriggerEvent(Long triggerEventId);
}
