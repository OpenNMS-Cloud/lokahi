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

        boolean notifyPagerDuty = false;
        boolean notifyEmail = false;

        for (MonitoringPolicy policy : dbPolicies) {
            if (policy.isNotifyByPagerDuty()) {
                notifyPagerDuty = true;
            }
            if (policy.isNotifyByEmail()) {
                notifyEmail = true;
            }
        }

        if (notifyPagerDuty) {
            postPagerDutyNotification(alert);
        }
        if (notifyEmail) {
            postEmailNotification(alert);
        }
    }

    private void postPagerDutyNotification(Alert alert) {
        try {
            pagerDutyAPI.postNotification(alert);
        } catch (NotificationException e) {
            log.warn("Unable to send alert[id: {}, tenant: {}] to PagerDuty:",
                alert.getDatabaseId(), alert.getTenantId(), e);
        }
    }

    private void postEmailNotification(Alert alert) {
        try {
            for (String emailAddress : keyCloakAPI.getTenantEmailAddresses(alert.getTenantId())) {
                String subject = String.format("%s severity alert",
                    StringUtils.capitalize(alert.getSeverity().getValueDescriptor().getName()));
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
