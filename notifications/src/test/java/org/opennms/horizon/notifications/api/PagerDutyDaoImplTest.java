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
import org.opennms.horizon.shared.dto.notifications.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class PagerDutyDaoImplTest {
    @InjectMocks
    PagerDutyDaoImpl pagerDutyDao;

    @Mock
    JdbcTemplate jdbcTemplate;

    @Test
    public void updateConfig() throws Exception {
        Mockito.when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(Integer.valueOf(1));
        PagerDutyConfigDTO config = getConfigDTO();
        pagerDutyDao.saveConfig(config);

        Mockito.verify(jdbcTemplate, times(1)).update(anyString(), anyString());
    }
    @Test
    public void insertConfig() throws Exception {
        Mockito.when(jdbcTemplate.queryForObject(anyString(), any(Class.class))).thenReturn(Integer.valueOf(0));
        PagerDutyConfigDTO config = getConfigDTO();
        pagerDutyDao.saveConfig(config);

        Mockito.verify(jdbcTemplate, times(1)).update(anyString(), anyString());
    }

    @Test
    public void getUninitialisedConfig() throws Exception {
        try {
            pagerDutyDao.getConfig();
        } catch (NotificationConfigUninitializedException e) {
            assertEquals("PagerDuty config not initialized. Row count=0", e.getMessage());
        }
    }

    @Test
    public void getInitialisedConfig() throws Exception {
        List<PagerDutyConfigDTO> configs = Arrays.asList(getConfigDTO());
        Mockito.when(jdbcTemplate.query(any(String.class), any(RowMapper.class))).thenReturn(configs);
        PagerDutyConfigDTO config = pagerDutyDao.getConfig();

        assertEquals("integration_key", config.getIntegrationkey());
    }

    @Test
    public void getUnInitialisedConfigNoTable() {
        doThrow(BadSqlGrammarException.class)
            .when(jdbcTemplate).query(any(String.class), any(RowMapper.class));

        boolean exceptionCaught = false;
        try{
            pagerDutyDao.getConfig();
        } catch (NotificationConfigUninitializedException e) {
            assertEquals("PagerDuty config not initialized. Table does not exist.", e.getMessage());
            exceptionCaught = true;
        }

        assertTrue(exceptionCaught);
    }

    private PagerDutyConfigDTO getConfigDTO() {
        return new PagerDutyConfigDTO("integration_key");
    }
}
