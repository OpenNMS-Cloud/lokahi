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
package org.opennms.horizon.events.persistence.repository;

import java.util.List;
import org.opennms.horizon.events.persistence.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByTenantId(String tenantId);

    List<Event> findAllByTenantIdAndNodeId(String tenantId, long nodeId);

    @Query(
            value = "SELECT e FROM Event e WHERE e.tenantId = :tenantId AND e.nodeId = :nodeId "
                    + " AND ( e.eventUei LIKE %:searchTerm% "
                    + " OR e.locationName LIKE %:searchTerm% "
                    + " OR e.description LIKE %:searchTerm% "
                    + " OR e.logMessage LIKE %:searchTerm% "
                    + " OR CAST( e.ipAddress  AS  string) LIKE %:searchTerm% )"
                    + " ORDER BY e.id",
            countQuery = "SELECT count(e)  FROM Event e  WHERE e.tenantId = :tenantId AND e.nodeId = :nodeId ")
    Page<Event> findByNodeIdAndSearchTermAndTenantId(
            @Param("tenantId") String tenantId,
            @Param("nodeId") Long nodeId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);
}
