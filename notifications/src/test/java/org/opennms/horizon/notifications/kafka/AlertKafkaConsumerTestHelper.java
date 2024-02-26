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
package org.opennms.horizon.notifications.kafka;

import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.notifications.tenant.WithTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AlertKafkaConsumerTestHelper {

    @Autowired
    private NotificationService notificationService;

    @WithTenant(tenantIdArg = 0)
    public void setupConfig(String tenantId) {
        // WithTenant annotation does not function for calls that are internal to the class, hence the need for a
        // helper.
        // It's due to the fact that Aspects use proxies that are only available for calls between different classes.
        // If you really wish to use an internal method, don't use the aspect, and use the line below:
        // try (TenantContext tc = TenantContext.withTenantId(tenantId)) {
        String integrationKey = "not_verified";

        PagerDutyConfigDTO config = PagerDutyConfigDTO.newBuilder()
                .setIntegrationKey(integrationKey)
                .setTenantId(tenantId)
                .build();
        notificationService.postPagerDutyConfig(config);
    }
}
