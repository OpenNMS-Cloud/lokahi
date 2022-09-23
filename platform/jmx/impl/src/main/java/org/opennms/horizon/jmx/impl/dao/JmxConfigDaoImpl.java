/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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


import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.opennms.horizon.config.service.api.ConfigConstants;
import org.opennms.horizon.config.service.api.ConfigService;
import org.opennms.horizon.jmx.config.JmxConfig;
import org.opennms.horizon.jmx.dao.JmxConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation for config dao class.
 *
 * @author Christian Pape <Christian.Pape@informatik.hs-fulda.de>
 */
public class JmxConfigDaoImpl implements JmxConfigDao {
    private static final Logger LOG = LoggerFactory.getLogger(JmxConfigDaoImpl.class);
    protected static final String JMX_CONFIG_EVENT = "jmx-config-init";
    ObjectMapper mapper = new ObjectMapper();
    private final ConfigService configService;

    public JmxConfigDaoImpl(ConfigService configService) {
        this.configService = configService;
    }

    public void initConfig() {
        Optional<String> jmxConfOp = configService.getConfig(ConfigConstants.JMX_CONFIG);
        if (jmxConfOp.isEmpty()) {
            try {
                URL url = this.getClass().getClassLoader().getResource("jmx-config.json");
                JmxConfig config = mapper.readValue(url, JmxConfig.class);
                configService.addConfig(ConfigConstants.JMX_CONFIG, mapper.writeValueAsString(config), JMX_CONFIG_EVENT);
            } catch (IOException e) {
                LOG.error("Failed to initial JMX config from jmx-config.json file", e);
            }
        }
    }

    /**
     * Returns the loaded config object.
     *
     * @return the current config object
     */
    @Override
    public JmxConfig getConfig() {
        Optional<String> jmxConfOp = configService.getConfig(ConfigConstants.JMX_CONFIG);
        if (!jmxConfOp.isEmpty()) {
            try {
                return mapper.readValue(jmxConfOp.get(), JmxConfig.class);
            } catch (JsonProcessingException e) {
                LOG.error("failed to map the config string {} to JmxConfig", jmxConfOp.get(), e);
                throw new RuntimeException(e);
            }
        } else {
            LOG.error("The JMX config has not been initialized");
            return null;
        }
    }

    //TODO: might be safe to remove this method
    public JmxConfig translateConfig(JmxConfig jmxConfig) {
        return jmxConfig;
    }
}
