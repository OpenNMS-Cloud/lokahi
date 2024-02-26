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

import java.util.Set;
import org.opennms.horizon.alertservice.db.entity.SystemPolicyTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemPolicyTagRepository extends JpaRepository<SystemPolicyTag, SystemPolicyTag.RelationshipId> {
    Set<SystemPolicyTag> findByTenantIdAndPolicyId(String tenantId, long policyId);

    @Modifying
    @Query("DELETE FROM SystemPolicyTag pt WHERE pt.tenantId = :tenantId and pt.policyId = :policyId and tag is null")
    void deleteEmptyTagByTenantIdAndPolicyId(@Param("tenantId") String tenantId, @Param("policyId") long policyId);
}
