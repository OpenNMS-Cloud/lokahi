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

import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private PagerDutyAPI pagerDutyAPI;

    @Override
    public void postNotification(AlarmDTO alarm) throws NotificationException {
        pagerDutyAPI.postNotification(alarm);
    }

    @Override
    @Async
    public void postPagerDutyConfig(PagerDutyConfigDTO config) {
        System.out.println("JH saving key="+config.getIntegrationKey());
        pagerDutyAPI.saveConfig(config);
        try {
            System.out.println("JH About to sleep");
            Thread.sleep(1000);
            System.out.println("JH Slept");

            String key = pagerDutyAPI.getPagerDutyIntegrationKey();
            System.out.println("JH retrieved key=" + key);
        } catch (Exception ex) {
            System.out.println("JH failed exception");
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void postPagerDutyConfigNonSpringAsync(PagerDutyConfigDTO config) {
        System.out.println("JH postPagerDutyConfigNonSpringAsync saving key="+config.getIntegrationKey());
        new Thread(new Runnable() {
            @Override
            public void run() {
                pagerDutyAPI.saveConfig(config);
            }
        }).start();
    }
}
