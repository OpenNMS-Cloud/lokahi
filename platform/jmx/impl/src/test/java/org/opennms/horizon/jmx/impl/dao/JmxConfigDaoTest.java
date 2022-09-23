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

package org.opennms.horizon.jmx.impl.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.opennms.horizon.config.service.api.ConfigConstants;
import org.opennms.horizon.config.service.api.ConfigService;
import org.opennms.horizon.jmx.config.JmxConfig;
import org.opennms.horizon.jmx.config.MBeanServer;

public class JmxConfigDaoTest {
    private ConfigService mockService;
    private JmxConfigDaoImpl configDao;
    private String configStr = "{\"mbeanServer\":[{\"ipAddress\":\"127.0.0.10\", \"port\":18980, \"parameters\":[{\"key\":\"protocol\",\"value\":\"rmi\"}]}]}";
    @Before
    public void setUp() {
        mockService = mock(ConfigService.class);
        configDao = new JmxConfigDaoImpl(mockService);
    }

    @Test
    public void testInitConfig() {
        doReturn(Optional.empty()).when(mockService).getConfig(ConfigConstants.JMX_CONFIG);
        configDao.initConfig();
        verify(mockService).getConfig(ConfigConstants.JMX_CONFIG);
        verify(mockService).addConfig(eq(ConfigConstants.JMX_CONFIG), anyString(), eq(JmxConfigDaoImpl.JMX_CONFIG_EVENT));
        verifyNoMoreInteractions(mockService);
    }


    @Test
    public void testGetConfig() {
        doReturn(Optional.of(configStr)).when(mockService).getConfig(ConfigConstants.JMX_CONFIG);
        JmxConfig config = configDao.getConfig();
        assertNotNull(config);
        assertEquals(1, config.getMBeanServer().size());
        MBeanServer mBeanServer = new ArrayList<MBeanServer>(config.getMBeanServer()).get(0);
        assertEquals("127.0.0.10", mBeanServer.getIpAddress());
        assertEquals(18980, mBeanServer.getPort());
        assertEquals(1, mBeanServer.getParameters().size());
        assertEquals("protocol", mBeanServer.getParameters().get(0).getKey());
        assertEquals("rmi", mBeanServer.getParameters().get(0).getValue());
        verify(mockService).getConfig(ConfigConstants.JMX_CONFIG);
        verifyNoMoreInteractions(mockService);
    }

    @Test
    public void testGetConfigNotFound() {
        doReturn(Optional.empty()).when(mockService).getConfig(ConfigConstants.JMX_CONFIG);
        JmxConfig config = configDao.getConfig();
        assertNull(config);
        verify(mockService).getConfig(ConfigConstants.JMX_CONFIG);
        verifyNoMoreInteractions(mockService);
    }
}
