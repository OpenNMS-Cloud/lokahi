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
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alertservice.db.entity.EventDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDefinitionRepository extends JpaRepository<EventDefinition, Long> {
    List<EventDefinition> findByEventType(EventType eventType);

    Optional<EventDefinition> findByEventTypeAndName(EventType eventType, String name);

    @Query(value = "SELECT DISTINCT vendor FROM event_definition WHERE vendor IS NOT NULL", nativeQuery = true)
    List<String> findDistinctVendors();

    List<EventDefinition> findByEventTypeAndVendor(EventType eventType, String vendor);
}
