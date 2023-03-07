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

package org.opennms.horizon.inventory.config;

import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {

    @Bean
    public AzureHttpParams azureHttpParams(@Value("${inventory.azure.login-url}") String loginUrl,
                                           @Value("${inventory.azure.management-url}") String managementUrl,
                                           @Value("${inventory.azure.api-version}") String apiVersion,
                                           @Value("${inventory.azure.metrics-api-version}") String metricsApiVersion) {

        AzureHttpParams params = new AzureHttpParams();
        params.setBaseLoginUrl(loginUrl);
        params.setBaseManagementUrl(managementUrl);
        params.setApiVersion(apiVersion);
        params.setApiVersion(metricsApiVersion);
        return params;
    }

    @Bean
    public AzureHttpClient azureHttpClient(AzureHttpParams params) {
        return new AzureHttpClient(params);
    }
}
