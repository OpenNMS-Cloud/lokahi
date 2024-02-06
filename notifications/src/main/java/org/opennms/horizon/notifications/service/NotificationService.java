/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.notifications.service;

import io.opentelemetry.api.trace.Span;
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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final PagerDutyAPI pagerDutyAPI;

    private final EmailAPI emailAPI;

    private final Velocity velocity;

    private final KeyCloakAPI keyCloakAPI;

    private final MonitoringPolicyRepository monitoringPolicyRepository;

    @WithTenant(tenantIdArg = 0, tenantIdArgInternalMethod = "getTenantId", tenantIdArgInternalClass = "org.opennms.horizon.alerts.proto.Alert")
    public void postNotification(Alert alert) {
        Span span = Span.current();
        span.setAttribute("user", alert.getTenantId());
        span.setAttribute("alertId", alert.getDatabaseId());

        if (alert.getMonitoringPolicyIdList().isEmpty()) {
            log.info("Alert has no associated monitoring policies, dropping alert[id: {}, tenant: {}]",
                alert.getDatabaseId(), alert.getTenantId());
            return;
        }

        List<MonitoringPolicy> dbPolicies = monitoringPolicyRepository.findByTenantIdAndIdIn(
            alert.getTenantId(),
            alert.getMonitoringPolicyIdList()
        );

        if (dbPolicies.isEmpty()) {
            log.warn("Associated policies {} not found, dropping alert[id: {}, tenant: {}]",
                alert.getMonitoringPolicyIdList(), alert.getDatabaseId(), alert.getTenantId());
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
        log.info("Alert[id: {}] monitoring policy ids: {}, notifyPagerDuty: {}, notifyEmail: {}",
            alert.getDatabaseId(), alert.getMonitoringPolicyIdList(), notifyPagerDuty, notifyEmail);

        if (notifyPagerDuty) {
            postPagerDutyNotification(alert);
        }
        if (notifyEmail) {
            postEmailNotification(alert);
        }
    }

    private void postPagerDutyNotification(Alert alert) {
        log.info("Sending alert[id: {}, tenant: {}] to PagerDuty",
            alert.getDatabaseId(), alert.getTenantId());
        try {
            pagerDutyAPI.postNotification(alert);
        } catch (NotificationException e) {
            log.warn("Unable to send alert[id: {}, tenant: {}] to PagerDuty:",
                alert.getDatabaseId(), alert.getTenantId(), e);
        }
    }

    private void postEmailNotification(Alert alert) {
        try {
            List<String> addresses = keyCloakAPI.getTenantEmailAddresses(alert.getTenantId());
            log.info("Emailing alert[id: {}, tenant: {}] to {} addresses",
                alert.getDatabaseId(), alert.getTenantId(), addresses.size());

            for (String emailAddress : addresses) {
                String subject = String.format("%s alert: %s",
                    StringUtils.capitalize(alert.getSeverity().getValueDescriptor().getName()),
                    alert.getNodeName());
                String htmlBody = velocity.populateTemplate(emailAddress, alert);

                emailAPI.sendEmail(emailAddress, subject, htmlBody);
            }
        } catch (NotificationException e) {
            log.warn("Unable to send alert[id: {}, tenant: {}] to Email:",
                alert.getDatabaseId(), alert.getTenantId(), e);
        }
    }

    public void postPagerDutyConfig(PagerDutyConfigDTO config) {
        pagerDutyAPI.saveConfig(config);
    }
}
