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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("SELECT a " +
        "FROM Alert a " +
        "LEFT JOIN FETCH a.alertCondition "+
        "WHERE a.tenantId = :tenantId " +
        "AND a.reductionKey = :reductionKey ")
    Optional<Alert> findByReductionKeyAndTenantId(@Param("reductionKey") String reductionKey, @Param("tenantId") String tenantId);

    List<Alert> findByReductionKeyStartingWithAndTenantId(String reductionKey, String tenantId);

    @Query(value = "SELECT a " +
        "FROM Alert a " +
        "LEFT JOIN FETCH a.alertCondition " +
        "LEFT JOIN FETCH a.alertCondition.rule " +
        "LEFT JOIN FETCH a.alertCondition.rule.policy " +
        "WHERE a.severity in (:severityList) " +
        "AND a.lastEventTime between :start and :end " +
        "AND a.managedObjectType = :managedObjectType " +
        "AND a.managedObjectInstance in (:managedObjectInstance) " +
        "AND a.tenantId = :tenantId ",
        countQuery = "SELECT count(a) " +
            "FROM Alert a " +
            "WHERE a.severity in (:severityList) " +
            "AND a.lastEventTime between :start and :end " +
            "AND a.managedObjectType = :managedObjectType " +
            "AND a.managedObjectInstance in (:managedObjectInstance) " +
            "AND a.tenantId = :tenantId ")
    Page<Alert> findBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(@Param("severityList") List<Severity> severityList, @Param("start") Date start, @Param("end") Date end, @Param("managedObjectType") ManagedObjectType managedObjectType, @Param("managedObjectInstance") List<String> managedObjectInstance, Pageable pageable, @Param("tenantId") String tenantId);

    int countBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(List<Severity> severityList, Date start, Date end, ManagedObjectType managedObjectType, List<String> managedObjectInstance, String tenantId);

    @Query(value = "SELECT a " +
        "FROM Alert a " +
        "LEFT JOIN FETCH a.alertCondition " +
        "LEFT JOIN FETCH a.alertCondition.rule " +
        "LEFT JOIN FETCH a.alertCondition.rule.policy " +
        "WHERE a.severity in (:severityList) " +
        "AND a.lastEventTime between :start and :end " +
        "AND a.tenantId = :tenantId ",
        countQuery = "SELECT count(a) " +
            "FROM Alert a " +
            "WHERE a.severity in (:severityList) " +
            "AND a.lastEventTime between :start and :end " +
            "AND a.tenantId = :tenantId ")
    Page<Alert> findBySeverityInAndLastEventTimeBetweenAndTenantId(@Param("severityList") List<Severity> severityList, @Param("start") Date start, @Param("end") Date end, Pageable pageable, @Param("tenantId") String tenantId);

    int countBySeverityInAndLastEventTimeBetweenAndTenantId(List<Severity> severityList, Date start, Date end, String tenantId);

    Optional<Alert> findByIdAndTenantId(long id, String tenantId);

    void deleteByIdAndTenantId(long databaseId, String tenantId);

    @Query(value = "SELECT a FROM Alert a LEFT JOIN AlertCondition ac LEFT JOIN PolicyRule r LEFT JOIN MonitorPolicy p "
        + "WHERE a.tenantId = :tenantId AND p.id = :policyId")
    List<Alert> findByPolicyIdAndTenantId(@Param("policyId") long policyId, @Param("tenantId") String tenantId);

    @Query(value = "SELECT a FROM Alert a LEFT JOIN AlertCondition ac LEFT JOIN PolicyRule r "
        + "WHERE a.tenantId = :tenantId AND r.id = :ruleId")
    List<Alert> findByRuleIdAndTenantId(@Param("ruleId") long ruleId, @Param("tenantId") String tenantId);

    @Query(value = "SELECT count(distinct a) FROM Alert a LEFT JOIN AlertCondition ac LEFT JOIN PolicyRule r LEFT JOIN MonitorPolicy p "
        + "WHERE a.tenantId = :tenantId AND p.id = :policyId")
    long countByPolicyIdAndTenantId(@Param("policyId") long policyId, @Param("tenantId") String tenantId);

    @Query(value = "SELECT count(distinct a) FROM Alert a LEFT JOIN AlertCondition ac LEFT JOIN PolicyRule r "
        + "WHERE a.tenantId = :tenantId AND r.id = :ruleId")
    long countByRuleIdAndTenantId(@Param("ruleId") long ruleId, @Param("tenantId") String tenantId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndSeverity(String tenantId, Severity severity);

    @Query(value = "SELECT a.severity as severity, COUNT(DISTINCT a.id) as count FROM Alert a WHERE a.tenantId = :tenantId GROUP BY a.severity")
    List<SeverityCount> countByTenantIdAndGroupBySeverity(@Param("tenantId") String tenantId);

    @Query(value = "SELECT count(distinct a) FROM Alert a WHERE a.tenantId = :tenantId AND a.acknowledgedByUser IS NOT NULL")
    long countByTenantIdAndAcknowledged(@Param("tenantId") String tenantId);

    @Query(value = "SELECT count(distinct a) FROM Alert a WHERE a.tenantId = :tenantId AND a.acknowledgedByUser IS NULL")
    long countByTenantIdAndUnAcknowledged(@Param("tenantId") String tenantId);
}
