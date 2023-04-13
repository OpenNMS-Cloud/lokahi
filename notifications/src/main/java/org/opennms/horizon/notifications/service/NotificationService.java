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

import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.api.email.EmailAPI;
import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.notifications.api.keycloak.KeyCloakAPI;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.model.MonitoringPolicy;
import org.opennms.horizon.notifications.repository.MonitoringPolicyRepository;
import org.opennms.horizon.notifications.tenant.WithTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private PagerDutyAPI pagerDutyAPI;

    @Autowired
    private EmailAPI emailAPI;

    @Autowired
    private KeyCloakAPI keyCloakAPI;

    @Autowired
    private MonitoringPolicyRepository monitoringPolicyRepository;

    @WithTenant(tenantIdArg = 0, tenantIdArgInternalMethod = "getTenantId", tenantIdArgInternalClass = "org.opennms.horizon.alerts.proto.Alert")
    public void postNotification(Alert alert) {
        List<MonitoringPolicy> dbPolicies = monitoringPolicyRepository.findByTenantIdAndIdIn(
            alert.getTenantId(),
            alert.getMonitoringPolicyIdList()
        );

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
            // Wrap in a try/catch, we don't want a failure to notify via PagerDuty to prevent us from sending an
            // email notification, etc.
            try {
                pagerDutyAPI.postNotification(alert);
            } catch (NotificationException e) {
                log.warn("Unable to send alert to PagerDuty: {}", alert, e);
            }
        }
        if (notifyEmail) {
            try {
                List<String> emailAddresses = keyCloakAPI.getTenantEmailAddresses(alert.getTenantId());
                if (!emailAddresses.isEmpty()) {
                    emailAPI.postNotification(emailAddresses, alert);
                }
            }catch (NotificationException e) {
                log.warn("Unable to send alert to Email: {}", alert, e);
            }
        }

        if (dbPolicies.isEmpty()) {
            log.debug("No monitoring policy found, dropping alert: {}", alert);
        }
    }

    public void postPagerDutyConfig(PagerDutyConfigDTO config) {
        pagerDutyAPI.saveConfig(config);
    }
}
