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
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.TenantCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByTenantId(String tenantId);

    Optional<Node> findByIdAndTenantId(long id, String tenantID);

    List<Node> findByNodeLabel(String label);

    List<Node> findByTenantIdAndMonitoredStateEquals(String tenantId, MonitoredState monitoredState);

    @Query("SELECT n " + "FROM Node n "
            + "WHERE n.tenantId = :tenantId "
            + "AND n.monitoringLocation.id = :location_id "
            + "AND n.nodeLabel = :nodeLabel ")
    Optional<Node> findByTenantLocationIdAndNodeLabel(
            @Param("tenantId") String tenantId,
            @Param("location_id") Long location,
            @Param("nodeLabel") String nodeLabel);

    @Query("SELECT n " + "FROM Node n "
            + "WHERE n.tenantId = :tenantId "
            + "AND (LOWER(n.nodeLabel) LIKE LOWER(CONCAT('%', :nodeLabelSearchTerm, '%'))"
            + "OR LOWER(n.nodeAlias) LIKE LOWER(CONCAT('%', :nodeLabelSearchTerm, '%')))")
    List<Node> findByTenantIdAndNodeLabelOrAliasLike(
            @Param("tenantId") String tenantId, @Param("nodeLabelSearchTerm") String nodeLabelSearchTerm);

    List<Node> findByIdInAndTenantId(List<Long> ids, String tenantId);

    @Query("SELECT n " + "FROM Node n "
            + "WHERE n.tenantId = :tenantId "
            + "AND n.monitoringLocation.id = :location_id "
            + "AND n.monitoredState = :monitoredState ")
    List<Node> findByTenantIdLocationsAndMonitoredStateEquals(
            @Param("tenantId") String tenantId,
            @Param("location_id") Long locationId,
            @Param("monitoredState") MonitoredState monitoredState);

    @Query("SELECT n " + "FROM Node n "
            + "WHERE n.tenantId = :tenantId "
            + "AND n.monitoringLocation.id = :location_id ")
    List<Node> findByTenantIdAndLocationId(@Param("tenantId") String tenantId, @Param("location_id") Long locationId);

    @Query("SELECT new org.opennms.horizon.inventory.model.TenantCount(n.tenantId, count(*)) " + "FROM Node n "
            + "GROUP BY n.tenantId")
    List<TenantCount> countNodesByTenant();

    @Query("SELECT DISTINCT n " + "FROM Node n "
            + "JOIN n.tags tag "
            + "WHERE n.tenantId = :tenantId "
            + "AND tag.name IN :tags")
    List<Node> findByTenantIdAndTagNamesIn(@Param("tenantId") String tenantId, @Param("tags") List<String> tags);

    List<Node> findByNodeAliasAndTenantId(String alias, String tenantId);

    @Query("SELECT COUNT(n.id) FROM Node n WHERE n.tenantId = :tenantId ")
    long countDistinctNodes(@Param("tenantId") String tenantId);
}
