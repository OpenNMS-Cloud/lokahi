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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.notifications.api.email.EmailAPI;
import org.opennms.horizon.notifications.api.email.Velocity;
import org.opennms.horizon.notifications.api.keycloak.KeyCloakAPI;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.model.MonitoringPolicy;
import org.opennms.horizon.notifications.repository.MonitoringPolicyRepository;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceImplTest {

    @InjectMocks
    NotificationService notificationService;

    @Mock
    PagerDutyAPI pagerDutyAPI;

    @Mock
    EmailAPI emailAPI;

    @Mock
    KeyCloakAPI keyCloakAPI;

    @Mock
    MonitoringPolicyRepository monitoringPolicyRepository;

    @Mock
    Velocity velocity;

    @Test
    public void testNotificationPagerDutyPolicy() throws NotificationException {
        // PagerDuty is enabled in the monitoring policy, resulting in a PagerDuty notification
        MonitoringPolicy monitoringPolicy = new MonitoringPolicy();
        monitoringPolicy.setId(1);
        monitoringPolicy.setTenantId("T1");
        monitoringPolicy.setNotifyByPagerDuty(true);
        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn("T1", List.of(1L)))
            .thenReturn(List.of(monitoringPolicy));

        Alert alert = Alert.newBuilder().setTenantId("T1").addMonitoringPolicyId(1).build();

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(1)).postNotification(alert);
        Mockito.verify(emailAPI, times(0)).sendEmail(any(), any(), any());
    }

    @Test
    public void testNotificationEmptyPolicy() throws NotificationException {
        // No notifications are enabled in the matching policy, so it is a no-op
        MonitoringPolicy monitoringPolicy = new MonitoringPolicy();
        monitoringPolicy.setTenantId("T1");
        monitoringPolicy.setId(1);
        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn("T1", List.of(1L)))
            .thenReturn(List.of(monitoringPolicy));

        Alert alert = Alert.newBuilder().setTenantId("T1").addMonitoringPolicyId(1).build();

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(0)).postNotification(alert);
        Mockito.verify(emailAPI, times(0)).sendEmail(any(), any(), any());
    }

    @Test
    public void testNotificationNoPolicy() throws NotificationException {
        // Either the alert didn't specify the notification policy, or we're unable to find the specified one, no-op.
        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn(anyString(), anyList())).thenReturn(List.of());

        Alert alert = Alert.newBuilder().setTenantId("T1").addMonitoringPolicyId(1).build();

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(0)).postNotification(alert);
        Mockito.verify(emailAPI, times(0)).sendEmail(any(), any(), any());
    }

    @Test
    public void testNotificationExceptionConsumed() throws NotificationException {
        MonitoringPolicy monitoringPolicy = new MonitoringPolicy();
        monitoringPolicy.setTenantId("T1");
        monitoringPolicy.setId(1);
        monitoringPolicy.setNotifyByPagerDuty(true);
        monitoringPolicy.setNotifyByEmail(true);
        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn("T1", List.of(1L)))
            .thenReturn(List.of(monitoringPolicy));
        List<String> adminEmail = List.of("admin@email");
        Mockito.when(keyCloakAPI.getTenantEmailAddresses("T1")).thenReturn(adminEmail);

        Alert alert = Alert.newBuilder().setTenantId("T1").addMonitoringPolicyId(1).build();
        doThrow(new NotificationAPIException("Foo")).when(pagerDutyAPI).postNotification(any());
        doThrow(new NotificationAPIException("Foo")).when(emailAPI).sendEmail(any(), any(), any());

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(1)).postNotification(alert);
        Mockito.verify(emailAPI, times(1)).sendEmail(any(), any(), any());
    }

    @Test
    public void testNotificationMatchesMultiplePolicies() throws NotificationException {
        MonitoringPolicy m1 = new MonitoringPolicy();
        m1.setTenantId("T1");
        m1.setId(1);
        m1.setNotifyByPagerDuty(true);

        MonitoringPolicy m2 = new MonitoringPolicy();
        m2.setTenantId("T1");
        m2.setId(2);
        m2.setNotifyByPagerDuty(true);

        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn("T1", List.of(1L, 2L)))
            .thenReturn(List.of(m1, m2));

        Alert alert = Alert.newBuilder().setTenantId("T1").addAllMonitoringPolicyId(List.of(1L, 2L)).build();

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(1)).postNotification(alert);
    }

    @Test
    public void testNotificationEmailPolicy() throws NotificationException {
        MonitoringPolicy monitoringPolicy = new MonitoringPolicy();
        monitoringPolicy.setId(1);
        monitoringPolicy.setTenantId("T1");
        monitoringPolicy.setNotifyByEmail(true);
        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn("T1", List.of(1L)))
            .thenReturn(List.of(monitoringPolicy));
        String adminEmail = "admin@email";
        Mockito.when(keyCloakAPI.getTenantEmailAddresses("T1")).thenReturn(List.of(adminEmail));

        Alert alert = Alert.newBuilder().setTenantId("T1").addMonitoringPolicyId(1).build();
        Mockito.when(velocity.populateTemplate(adminEmail, alert)).thenReturn("Alert email");

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(0)).postNotification(alert);
        Mockito.verify(emailAPI, times(1)).sendEmail(eq(adminEmail), any(), eq("Alert email"));
    }

    @Test
    public void testNotificationEmailPolicyNoTenantEmails() throws NotificationException {
        MonitoringPolicy monitoringPolicy = new MonitoringPolicy();
        monitoringPolicy.setId(1);
        monitoringPolicy.setTenantId("T1");
        monitoringPolicy.setNotifyByEmail(true);
        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn("T1", List.of(1L)))
            .thenReturn(List.of(monitoringPolicy));
        List<String> adminEmail = Collections.emptyList();
        Mockito.when(keyCloakAPI.getTenantEmailAddresses("T1")).thenReturn(adminEmail);

        Alert alert = Alert.newBuilder().setTenantId("T1").addMonitoringPolicyId(1).build();

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(0)).postNotification(alert);
        Mockito.verify(emailAPI, times(0)).sendEmail(any(), any(), any());
    }

    @Test
    public void testNotificationEmailAndPagerDuty() throws NotificationException {
        MonitoringPolicy monitoringPolicy = new MonitoringPolicy();
        monitoringPolicy.setId(1);
        monitoringPolicy.setTenantId("T1");
        monitoringPolicy.setNotifyByPagerDuty(true);
        monitoringPolicy.setNotifyByEmail(true);
        Mockito.when(monitoringPolicyRepository.findByTenantIdAndIdIn("T1", List.of(1L)))
            .thenReturn(List.of(monitoringPolicy));
        String adminEmail = "admin@email";
        Mockito.when(keyCloakAPI.getTenantEmailAddresses("T1")).thenReturn(List.of(adminEmail));

        Alert alert = Alert.newBuilder().setTenantId("T1").addMonitoringPolicyId(1).build();
        Mockito.when(velocity.populateTemplate(adminEmail, alert)).thenReturn("Alert email");

        notificationService.postNotification(alert);

        Mockito.verify(pagerDutyAPI, times(1)).postNotification(alert);
        Mockito.verify(emailAPI, times(1)).sendEmail(eq(adminEmail), any(), eq("Alert email"));
    }

    @Test
    public void testPostPagerDutyConfig() throws Exception {
        notificationService.postPagerDutyConfig(null);
    }
}
