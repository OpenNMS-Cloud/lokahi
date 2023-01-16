/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigUpdateService {

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("new-location-run-config-update-%d")
        .build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);
    private final TrapConfigService trapConfigService;
    private final FlowsConfigService flowsConfigService;

    private void sendConfigUpdatesToMinion(String tenantId, String location) {
        try {
            trapConfigService.sendTrapConfigToMinion(tenantId, location);
        } catch (Exception e) {
            log.error("Exception while sending traps to Minion", e);
        }
        try {
            flowsConfigService.sendFlowsConfigToMinion(tenantId, location);
        } catch (Exception e) {
            log.error("Exception while sending flows to Minion", e);
        }
    }

    public void sendConfigUpdate(String tenantId, String location) {
        executorService.execute(() -> sendConfigUpdatesToMinion(tenantId, location));
    }

}
