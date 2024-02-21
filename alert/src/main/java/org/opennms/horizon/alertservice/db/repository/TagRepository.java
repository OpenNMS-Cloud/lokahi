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
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query("select tag from Tag tag join tag.policies policy where tag.tenantId =:tenantId and policy.id = :policyId")
    List<Tag> findByTenantIdAndPolicyId(@Param("tenantId") String tenantID, @Param("policyId") Long policyId);

    List<Tag> findByTenantId(String tenantId);

    Optional<Tag> findByTenantIdAndName(String tenantId, String name);

    @Query(value = "SELECT * FROM tag WHERE tenant_id = :tenantId AND node_ids @> ARRAY[:nodeId]", nativeQuery = true)
    List<Tag> findByTenantIdAndNodeId(@Param("tenantId") String tenantId, @Param("nodeId") Long nodeId);
}
