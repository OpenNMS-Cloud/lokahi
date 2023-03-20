/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.cucumber.steps;

import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.junit.jupiter.api.Assertions;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcmpDiscoveryStepDefinitions {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryProcessingStepDefinitions.class);
    private static InventoryBackgroundHelper backgroundHelper;
    private IcmpActiveDiscoveryCreateDTO icmpDiscovery;

    @BeforeAll
    public static void beforeAll() {
        backgroundHelper = new InventoryBackgroundHelper();
    }

    @Given("[ICMP Discovery] External GRPC Port in system property {string}")
    public void icmpDiscoveryExternalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[ICMP Discovery] Kafka Bootstrap URL in system property {string}")
    public void icmpDiscoveryKafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[ICMP Discovery] MOCK Minion Gateway Base URL in system property {string}")
    public void icmpDiscoveryMOCKMinionGatewayBaseURLInSystemProperty(String arg0) {
    }

    @Given("[ICMP Discovery] Grpc TenantId {string}")
    public void icmpDiscoveryGrpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[ICMP Discovery] Create Grpc Connection for Inventory")
    public void icmpDiscoveryCreateGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    @Given("New Active Discovery with IpAddresses {string} and SNMP community as {string} at location {string}")
    public void newActiveDiscoveryWithIpAddressesAndSNMPCommunityAsAtLocation(String ipAddressStrings, String snmpReadCommunity, String location) {
        icmpDiscovery = IcmpActiveDiscoveryCreateDTO.newBuilder()
            .addIpAddresses(ipAddressStrings).setSnmpConf(SNMPConfigDTO.newBuilder().addReadCommunity(snmpReadCommunity).build())
            .setLocation(location).build();
    }


    @Then("create Active Discovery and validate it's created active discovery with above details.")
    public void createActiveDiscoveryAndValidateItSCreatedActiveDiscoveryWithAboveDetails() {
        var icmpDiscoveryDto = backgroundHelper.getIcmpActiveDiscoveryServiceBlockingStub().createDiscovery(icmpDiscovery);
        Assertions.assertEquals(icmpDiscovery.getLocation(), icmpDiscoveryDto.getLocation());
        Assertions.assertEquals(icmpDiscovery.getIpAddresses(0), icmpDiscoveryDto.getIpAddresses(0));
        Assertions.assertEquals(icmpDiscovery.getSnmpConf().getReadCommunity(0), icmpDiscoveryDto.getSnmpConf().getReadCommunity(0));
    }


}
