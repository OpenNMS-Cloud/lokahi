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
package org.opennms.horizon.alertservice.api;

import java.util.List;
import java.util.Optional;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertCount;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.inventory.dto.NodeDTO;

public interface AlertService {
    List<Alert> reduceEvent(Event e);

    boolean deleteByIdAndTenantId(long id, String tenantId);

    void deleteByTenantId(Alert alert, String tenantId);

    Optional<Alert> acknowledgeByIdAndTenantId(long id, String tenantId);

    Optional<Alert> unacknowledgeByIdAndTenantId(long id, String tenantId);

    Optional<Alert> escalateByIdAndTenantId(long id, String tenantId);

    Optional<Alert> clearByIdAndTenantId(long id, String tenantId);

    void addListener(AlertLifecycleListener listener);

    void removeListener(AlertLifecycleListener listener);

    void saveNode(NodeDTO node);

    AlertCount getAlertsCount(String tenantId);
}
