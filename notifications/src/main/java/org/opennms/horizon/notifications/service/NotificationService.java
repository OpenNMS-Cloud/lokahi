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

import io.opentelemetry.api.trace.Span;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.notifications.api.email.EmailAPI;
import org.opennms.horizon.notifications.api.email.Velocity;
import org.opennms.horizon.notifications.api.keycloak.KeyCloakAPI;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.model.MonitoringPolicy;
import org.opennms.horizon.notifications.repository.MonitoringPolicyRepository;
import org.opennms.horizon.notifications.tenant.WithTenant;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final PagerDutyAPI pagerDutyAPI;

    private final EmailAPI emailAPI;

    private final Velocity velocity;

    private final KeyCloakAPI keyCloakAPI;

    private final MonitoringPolicyRepository monitoringPolicyRepository;

    @WithTenant(
            tenantIdArg = 0,
            tenantIdArgInternalMethod = "getTenantId",
            tenantIdArgInternalClass = "org.opennms.horizon.alerts.proto.Alert")
    public void postNotification(Alert alert) {
        Span span = Span.current();
        span.setAttribute("user", alert.getTenantId());
        span.setAttribute("alertId", alert.getDatabaseId());

        if (alert.getMonitoringPolicyIdList().isEmpty()) {
            log.info(
                    "Alert has no associated monitoring policies, dropping alert[id: {}, tenant: {}]",
                    alert.getDatabaseId(),
                    alert.getTenantId());
            return;
        }

        List<MonitoringPolicy> dbPolicies = monitoringPolicyRepository.findByTenantIdAndIdIn(
                alert.getTenantId(), alert.getMonitoringPolicyIdList());

        if (dbPolicies.isEmpty()) {
            log.warn(
                    "Associated policies {} not found, dropping alert[id: {}, tenant: {}]",
                    alert.getMonitoringPolicyIdList(),
                    alert.getDatabaseId(),
                    alert.getTenantId());
            return;
        }

        boolean notifyEmail = false;
        boolean notifyPagerDuty = false;

        for (MonitoringPolicy policy : dbPolicies) {
            if (policy.isNotifyByPagerDuty()) {
                notifyPagerDuty = true;
            }
            if (policy.isNotifyByEmail()) {
                notifyEmail = true;
            }
        }
        log.info(
                "Alert[id: {}] monitoring policy ids: {}, notifyPagerDuty: {}, notifyEmail: {}",
                alert.getDatabaseId(),
                alert.getMonitoringPolicyIdList(),
                notifyPagerDuty,
                notifyEmail);

        if (notifyPagerDuty) {
            postPagerDutyNotification(alert);
        }
        if (notifyEmail) {
            postEmailNotification(alert);
        }
    }

    private void postPagerDutyNotification(Alert alert) {
        log.info("Sending alert[id: {}, tenant: {}] to PagerDuty", alert.getDatabaseId(), alert.getTenantId());
        try {
            pagerDutyAPI.postNotification(alert);
        } catch (NotificationException e) {
            log.warn(
                    "Unable to send alert[id: {}, tenant: {}] to PagerDuty:",
                    alert.getDatabaseId(),
                    alert.getTenantId(),
                    e);
        }
    }

    private void postEmailNotification(Alert alert) {
        try {
            List<String> addresses = keyCloakAPI.getTenantEmailAddresses(alert.getTenantId());
            log.info(
                    "Emailing alert[id: {}, tenant: {}] to {} addresses",
                    alert.getDatabaseId(),
                    alert.getTenantId(),
                    addresses.size());

            for (String emailAddress : addresses) {
                String subject = String.format(
                        "%s alert: %s",
                        StringUtils.capitalize(
                                alert.getSeverity().getValueDescriptor().getName()),
                        alert.getNodeName());
                String htmlBody = velocity.populateTemplate(emailAddress, alert);

                emailAPI.sendEmail(emailAddress, subject, htmlBody);
            }
        } catch (NotificationException e) {
            log.warn(
                    "Unable to send alert[id: {}, tenant: {}] to Email:",
                    alert.getDatabaseId(),
                    alert.getTenantId(),
                    e);
        }
    }

    public void postPagerDutyConfig(PagerDutyConfigDTO config) {
        pagerDutyAPI.saveConfig(config);
    }
}
