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

import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.db.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByReductionKeyAndTenantId(String reductionKey, String tenantId);

    List<Alert> findByReductionKeyStartingWithAndTenantId(String reductionKey, String tenantId);

    Page<Alert> findBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(List<Severity> severityList, Date start, Date end, ManagedObjectType managedObjectType, List<String> managedObjectInstance, Pageable pageable, String tenantId);

    int countBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(List<Severity> severityList, Date start, Date end, ManagedObjectType managedObjectType, List<String> managedObjectInstance, String tenantId);

    Page<Alert> findBySeverityInAndLastEventTimeBetweenAndTenantId(List<Severity> severityList, Date start, Date end, Pageable pageable, String tenantId);

    int countBySeverityInAndLastEventTimeBetweenAndTenantId(List<Severity> severityList, Date start, Date end, String tenantId);

    Optional<Alert> findByIdAndTenantId(long id, String tenantId);

    void deleteByIdAndTenantId(long databaseId, String tenantId);
}
