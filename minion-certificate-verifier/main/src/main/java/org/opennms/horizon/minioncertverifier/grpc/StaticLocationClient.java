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

package org.opennms.horizon.minioncertverifier.grpc;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("static")
public class StaticLocationClient implements LocationClient {

    private Map<String, Set<MonitoringLocationDTO>> tenantLocations = new HashMap<>();

    public StaticLocationClient(@Value("${inventory.file}") URL location) throws IOException {
        Properties properties = new Properties();
        properties.load(location.openStream());

        for (String key : properties.stringPropertyNames()) {
            String locationValues = Optional.ofNullable(properties.getProperty(key))
                .map(String::trim)
                .orElse("");
            Set<MonitoringLocationDTO> locations = tenantLocations.computeIfAbsent(key, (ignored) -> new HashSet<>());
            Arrays.stream(locationValues.split(";"))
                .map(String::trim)
                .map(str -> str.split(","))
                .filter(arr -> arr.length == 2)
                .map(arr -> MonitoringLocationDTO.newBuilder()
                    .setId(Long.parseLong(arr[0]))
                    .setLocation(arr[1])
                    .setTenantId(key)
                    .build()
                )
                .forEach(locations::add);
        }
    }

    @Override
    public Optional<MonitoringLocationDTO> getLocation(String tenantId, long locationId) {
        if (!tenantLocations.containsKey(tenantId)) {
            return Optional.empty();
        }

        for (MonitoringLocationDTO location : tenantLocations.get(tenantId)) {
            if (locationId == location.getId()) {
                return Optional.of(location);
            }
        }
        return Optional.empty();
    }
}
