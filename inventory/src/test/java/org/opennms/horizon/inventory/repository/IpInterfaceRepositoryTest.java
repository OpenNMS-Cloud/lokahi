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

package org.opennms.horizon.inventory.repository;


import com.vladmihalcea.hibernate.type.basic.Inet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class IpInterfaceRepositoryTest {

    private static final int NUM_NODES = 10;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private IpInterfaceRepository ipInterfaceRepository;

    @Autowired
    private MonitoringLocationRepository monitoringLocationRepository;

    @BeforeEach
    public void setup() {
        loadNodes();
    }

    @Test
    void testFindByIpInterfaceForAGivenLocationAndIpAddress() {
        var node = nodeRepository.findByNodeLabel("node1");
        assertNotNull(node);
        var locationList = monitoringLocationRepository.findByLocation("location1");
        assertFalse(locationList.isEmpty(), "Should have valid location");
        var list = ipInterfaceRepository.findAll();
        assertThat(list).isNotEmpty();

        for (int i = 0; i < NUM_NODES; i++) {
            var optional = ipInterfaceRepository.findByIpAddressAndLocationAndTenantId(
                new Inet("192.168.1." + i), "location" + i, "tenant" + i);
            assertThat(optional).isNotEmpty();
            assertThat(optional.get().getIpAddress()).isEqualTo(new Inet("192.168.1." + i));

            // Check with invalid location
            var optionalInterface = ipInterfaceRepository.findByIpAddressAndLocationAndTenantId(
                new Inet("192.168.1." + i), "location" + i + 3, "tenant" + i);
            assertThat(optionalInterface).isEmpty();

            // Check with invalid tenant
            optionalInterface = ipInterfaceRepository.findByIpAddressAndLocationAndTenantId(
                new Inet("192.168.1." + i), "location" + i, "tenant2" + i + 3);
            assertThat(optionalInterface).isEmpty();
        }

    }

    private void loadNodes() {
        for (int i = 0; i < NUM_NODES; i++) {
            var node = new Node();
            node.setNodeLabel("node" + i);
            node.setCreateTime(LocalDateTime.now());
            node.setTenantId("tenant" + i);
            var location = new MonitoringLocation();
            location.setLocation("location" + i);
            location.setTenantId("tenant" + i);
            monitoringLocationRepository.save(location);
            node.setMonitoringLocation(location);
            var ipInterface = new IpInterface();
            ipInterface.setTenantId("tenant" + i);
            ipInterface.setNode(node);
            ipInterface.setIpAddress(new Inet("192.168.1." + i));
            var ipInterfaces = new ArrayList<IpInterface>();
            ipInterfaces.add(ipInterface);
            node.setIpInterfaces(ipInterfaces);
            nodeRepository.save(node);
        }
    }

}
