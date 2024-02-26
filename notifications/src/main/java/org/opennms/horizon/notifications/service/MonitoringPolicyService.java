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
package org.opennms.horizon.notifications.service;

import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.notifications.mapper.MonitoringPolicyMapper;
import org.opennms.horizon.notifications.model.MonitoringPolicy;
import org.opennms.horizon.notifications.repository.MonitoringPolicyRepository;
import org.opennms.horizon.notifications.tenant.WithTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringPolicyService {

    @Autowired
    private MonitoringPolicyMapper monitoringPolicyMapper;

    @Autowired
    private MonitoringPolicyRepository monitoringPolicyRepository;

    @WithTenant(
            tenantIdArg = 0,
            tenantIdArgInternalMethod = "getTenantId",
            tenantIdArgInternalClass = "org.opennms.horizon.alerts.proto.MonitorPolicyProto")
    public void saveMonitoringPolicy(MonitorPolicyProto monitoringPolicyProto) {
        // Assuming updates always arrive in order on Kafka...
        MonitoringPolicy policy = monitoringPolicyMapper.dtoToModel(monitoringPolicyProto);
        monitoringPolicyRepository.save(policy);
    }
}
