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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;

class StaticLocationClientTest {

    private LocationClient client;

    @BeforeEach
    public void setUp() throws Exception {
//        Properties properties = new Properties();
//        properties.load(getClass().getResourceAsStream("/tenant_locations.properties"));
        client = new StaticLocationClient(getClass().getResource("/tenant_locations.properties"));
    }

    @Test
    public void verifyStaticLookup() {
        assertThat(client.getLocation("opennms-prime", 222L))
            .isNotEmpty().contains(createLocation("opennms-prime", "x-loc-x", 222L)
        );
    }

    private static MonitoringLocationDTO createLocation(String value, String location, long locationId) {
        return MonitoringLocationDTO.newBuilder()
            .setTenantId(value)
            .setLocation(location)
            .setId(locationId)
            .build();
    }

    @Test
    public void verifyStaticLookupWithNonDefaultTenant() {
        assertThat(client.getLocation("x-tenant-x", 222L))
            .isNotEmpty().contains(createLocation("x-tenant-x", "x-loc-x", 222L)
        );
    }

    @Test
    public void verifyStaticLookupWithNonDefaultTenantAndUnknownLocation() {
        assertThat(client.getLocation("x-tenant-x", 333L)).isEmpty();
    }

    @Test
    public void verifyStaticLookupWithUnknownTenant() {
        assertThat(client.getLocation("unknown-tenant", 222L))
            .isEmpty();
    }

}
