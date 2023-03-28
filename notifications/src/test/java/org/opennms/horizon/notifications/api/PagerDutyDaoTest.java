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

package org.opennms.horizon.notifications.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.opennms.horizon.notifications.mapper.PagerDutyConfigMapper;
import org.opennms.horizon.notifications.model.PagerDutyConfig;
import org.opennms.horizon.notifications.repository.PagerDutyConfigRepository;
import org.opennms.horizon.notifications.tenant.WithTenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class PagerDutyDaoTest {
    @InjectMocks
    PagerDutyDao pagerDutyDao;

    @Mock
    PagerDutyConfigRepository pagerDutyConfigRepository;

    @Mock
    PagerDutyConfigMapper pagerDutyConfigMapper;

    @Test
    public void updateConfig() throws Exception {
        Mockito.when(pagerDutyConfigRepository.findByTenantId(any())).thenReturn(Arrays.asList(new PagerDutyConfig()));
        PagerDutyConfigDTO config = getConfigDTO();
        pagerDutyDao.saveConfig(config);

        Mockito.verify(pagerDutyConfigRepository, times(1)).save(any());
    }
    @Test
    public void insertConfig() throws Exception {
        Mockito.when(pagerDutyConfigRepository.findByTenantId(any())).thenReturn(new ArrayList<>());
        PagerDutyConfigDTO config = getConfigDTO();
        pagerDutyDao.saveConfig(config);

        Mockito.verify(pagerDutyConfigRepository, times(1)).save(any());
    }

    @Test
    public void getUninitialisedConfig() {
        try {
            pagerDutyDao.getConfig("any");
        } catch (NotificationConfigUninitializedException e) {
            assertEquals("PagerDuty config not initialized. Row count=0", e.getMessage());
        }
    }

    @Test
    public void getInitialisedConfig() throws Exception {
        List<PagerDutyConfig> configs = Arrays.asList(new PagerDutyConfig());
        Mockito.when(pagerDutyConfigRepository.findByTenantId("any")).thenReturn(configs);
        Mockito.when(pagerDutyConfigMapper.modelToDTO(any())).thenReturn(getConfigDTO());
        PagerDutyConfigDTO config = pagerDutyDao.getConfig("any");

        assertEquals("integration_key", config.getIntegrationKey());
    }

    private PagerDutyConfigDTO getConfigDTO() {
        return PagerDutyConfigDTO.newBuilder().setIntegrationKey("integration_key").build();
    }
}
