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


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import io.grpc.Context;

@SpringBootTest
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
    @Autowired
    private SnmpInterfaceRepository snmpInterfaceRepository;

    @BeforeEach
    public void setup() throws UnknownHostException {
        loadNodes();
    }

    @AfterEach
    public void cleanUp() {
        IntStream.range(0, NUM_NODES).forEach(i ->
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, "tenant" + i).run(()-> {
            nodeRepository.deleteAll();
            ipInterfaceRepository.deleteAll();
            monitoringLocationRepository.deleteAll();
        }));
    }

    @Test
    void testFindByIpInterfaceForAGivenLocationAndIpAddress() throws UnknownHostException {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, "tenant1").run(()->
        {
            var node = nodeRepository.findByNodeLabel("node1");
            assertNotNull(node);
            var locationList = monitoringLocationRepository.findByLocation("location1");
            assertFalse(locationList.isEmpty(), "Should have valid location");
            var list = ipInterfaceRepository.findAll();
            assertThat(list).isNotEmpty();
        });

        for (int count = 0; count < NUM_NODES; count++) {
            final int i = count;
            final InetAddress inetAddress = InetAddress.getByName("192.168.1." + i);
            final String tenant = "tenant" + i;

            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenant).run(()->
            {
                var optional = ipInterfaceRepository.findByIpAddressAndLocationAndTenantId(
                    inetAddress, "location" + i, tenant);
                assertThat(optional).isNotEmpty();
                assertThat(optional.get().getIpAddress()).isEqualTo(inetAddress);

                // Check with invalid location
                var optionalInterface = ipInterfaceRepository.findByIpAddressAndLocationAndTenantId(
                    inetAddress, "location" + i + 3, tenant);
                assertThat(optionalInterface).isEmpty();
            });

            final String invalidTenant = "tenant2" + i + 3;
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, invalidTenant).run(()->
            {
                // Check with invalid tenant
                var optionalInterface = ipInterfaceRepository.findByIpAddressAndLocationAndTenantId(
                    inetAddress, "location" + i, invalidTenant);
                assertThat(optionalInterface).isEmpty();
            });
        }

    }

    @Test
    void testIpInterfaceWithSNMPIfDeleteSNMP() throws UnknownHostException {
        int ifIndex = 10;
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, "tenant1").run(() ->{
            var node = nodeRepository.findByNodeLabel("node1").get(0);
            var snmpInterface = new SnmpInterface();
            snmpInterface.setNode(node);
            snmpInterface.setIfIndex(ifIndex);
            snmpInterface.setIfName("test-snmp");
            snmpInterface.setTenantId(node.getTenantId());
            snmpInterfaceRepository.save(snmpInterface);
            var ipInterface = new IpInterface();
            ipInterface.setNode(node);
            ipInterface.setSnmpInterface(snmpInterface);
            ipInterface.setTenantId(node.getTenantId());
            try {
                ipInterface.setIpAddress(InetAddress.getByName("127.0.0.1"));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            ipInterfaceRepository.save(ipInterface);
            var result = ipInterfaceRepository.findById(ipInterface.getId());
            assertThat(result).isPresent().get()
                .extracting(i -> i.getIpAddress().getHostAddress(), i -> i.getSnmpInterface().getIfName())
                .containsExactly("127.0.0.1", "test-snmp");
            //test after snmp being deleted
            snmpInterfaceRepository.deleteById(snmpInterface.getId());
            var result2 = ipInterfaceRepository.findById(ipInterface.getId());
            assertThat(result2).isPresent();
        });
    }

    @Test
    void testIpInterfaceWithSNMPIfDeleteIPIf() throws UnknownHostException {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, "tenant1").run(() -> {
            int ifIndex = 10;
            var node = nodeRepository.findByNodeLabel("node1").get(0);
            var snmpInterface = new SnmpInterface();
            snmpInterface.setNode(node);
            snmpInterface.setIfIndex(ifIndex);
            snmpInterface.setIfName("test-snmp");
            snmpInterface.setTenantId(node.getTenantId());
            snmpInterfaceRepository.save(snmpInterface);
            var ipInterface = new IpInterface();
            ipInterface.setNode(node);
            ipInterface.setSnmpInterface(snmpInterface);
            ipInterface.setTenantId(node.getTenantId());
            try {
                ipInterface.setIpAddress(InetAddress.getByName("127.0.0.1"));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            ipInterfaceRepository.save(ipInterface);
            ipInterfaceRepository.deleteById(ipInterface.getId());
            var snmpIf = snmpInterfaceRepository.findById(snmpInterface.getId());
            assertThat(snmpIf).isPresent();
        });
    }

    private void loadNodes() {
        for (int count = 0; count < NUM_NODES; count++) {
            final int i = count;
            final String tenant = "tenant" + i;
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenant).run(()->
            {
                var node = new Node();
                node.setNodeLabel("node" + i);
                node.setCreateTime(LocalDateTime.now());
                node.setTenantId(tenant);
                var location = new MonitoringLocation();
                location.setLocation("location" + i);
                location.setTenantId(tenant);
                monitoringLocationRepository.save(location);
                node.setMonitoringLocation(location);
                var ipInterface = new IpInterface();
                ipInterface.setTenantId(tenant);
                ipInterface.setNode(node);
                try {
                    ipInterface.setIpAddress(InetAddress.getByName("192.168.1." + i));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
                var ipInterfaces = new ArrayList<IpInterface>();
                ipInterfaces.add(ipInterface);
                node.setIpInterfaces(ipInterfaces);
                nodeRepository.save(node);
            });
        }
    }

}
