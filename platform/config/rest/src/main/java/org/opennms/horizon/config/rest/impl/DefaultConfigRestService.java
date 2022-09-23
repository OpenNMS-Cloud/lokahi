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

package org.opennms.horizon.config.rest.impl;

import org.opennms.horizon.config.rest.api.ConfigRestService;
import org.opennms.horizon.config.service.api.ConfigService;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

public class DefaultConfigRestService implements ConfigRestService {

    private static final String CONFIG_SOURCE_REST = "core-rest";
    private final ConfigService configService;

    public DefaultConfigRestService(ConfigService configService) {
        this.configService = configService;
    }


    @Override
    public Response getConfig(String configName) {
        Optional<String> jsonConfig = configService.getConfig(configName);
        if (jsonConfig.isPresent()) {
            return Response.ok(jsonConfig.get()).build();
        }
        return Response.noContent().build();
    }

    @Override
    public Response getAllConfigNames() {
        List<String> configNames = configService.getConfigNames();
        if (configNames.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(configNames).build();
    }

    @Override
    public Response updateConfig(String configName, String jsonConfig) {
        try {
            configService.updateConfig(configName, jsonConfig, CONFIG_SOURCE_REST);
        } catch (Exception e) {
            Response.status(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage()).build();
        }
        return Response.accepted().build();
    }

    @Override
    public Response addConfig(String configName, String jsonConfig) {
        try {
            configService.addConfig(configName, jsonConfig, CONFIG_SOURCE_REST);
        } catch (Exception e) {
            Response.status(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage()).build();
        }
        return Response.accepted().build();
    }
}
