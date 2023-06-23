/*
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
 *
 */

package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.TenantCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByTenantId(String tenantId);
    Optional<Node> findByIdAndTenantId(long id, String tenantID);
    List<Node> findByNodeLabel(String label);
    List<Node> findByTenantIdAndMonitoredStateEquals(String tenantId, MonitoredState monitoredState);

    @Query("SELECT n " +
        "FROM Node n " +
        "WHERE n.tenantId = :tenantId " +
        "AND n.monitoringLocation.id = :location_id " +
        "AND n.nodeLabel = :nodeLabel ")
    Optional<Node> findByTenantLocationIdAndNodeLabel(@Param("tenantId") String tenantId,
                                                    @Param("location_id") Long location,
                                                    @Param("nodeLabel") String nodeLabel);

    @Query("SELECT n " +
        "FROM Node n " +
        "WHERE n.tenantId = :tenantId " +
        "AND LOWER(n.nodeLabel) LIKE LOWER(CONCAT('%', :nodeLabelSearchTerm, '%'))")
    List<Node> findByTenantIdAndNodeLabelLike(@Param("tenantId") String tenantId,
                                              @Param("nodeLabelSearchTerm") String nodeLabelSearchTerm);

    List<Node> findByIdInAndTenantId(List<Long> ids, String tenantId);


    @Query("SELECT n " +
        "FROM Node n " +
        "WHERE n.tenantId = :tenantId " +
        "AND n.monitoringLocation.id = :location_id " +
        "AND n.monitoredState = :monitoredState ")
    List<Node> findByTenantIdLocationsAndMonitoredStateEquals(@Param("tenantId") String tenantId,
                                                              @Param("location_id") Long locationId,
                                                              @Param("monitoredState") MonitoredState monitoredState);

    @Query("SELECT new org.opennms.horizon.inventory.model.TenantCount(n.tenantId, count(*)) " +
        "FROM Node n " +
        "GROUP BY n.tenantId"
    )
    List<TenantCount> countNodesByTenant();

    @Query("SELECT DISTINCT n " +
        "FROM Node n " +
        "JOIN n.tags tag " +
        "WHERE n.tenantId = :tenantId " +
        "AND tag.name IN :tags")
    List<Node> findByTenantIdAndTagNamesIn(@Param("tenantId") String tenantId,
                                           @Param("tags") List<String> tags);
}
