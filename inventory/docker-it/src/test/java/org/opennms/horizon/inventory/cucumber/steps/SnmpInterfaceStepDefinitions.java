package org.opennms.horizon.inventory.cucumber.steps;


import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.dto.SnmpInterfaceCreateDTO;
import org.opennms.horizon.inventory.dto.SnmpInterfacesList;
import org.opennms.horizon.inventory.dto.SearchBy;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class SnmpInterfaceStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;

    public SnmpInterfaceStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }


    @Given("a new snmp_interface with name {string}, desc {string} ,alias {string} , physical address {string} and node label {string} with tenant {string}")
    public void aNewSnmp_interfaceWithNameDescAliasPhysicalAddressAndNodeIdWithTenant(String name, String desc, String alias, String phyAddr, String nodelabel, String tenant) {
        aNewSnmpInterface(name, desc, alias, phyAddr, nodelabel, tenant);
    }

    private void aNewSnmpInterface(String name, String desc, String alias, String physicalAddr, String nodelabel, String tenantId) {

        var snmpCreateDTO = SnmpInterfaceCreateDTO.newBuilder();
        snmpCreateDTO
            .setIfAlias(alias)
            .setTenantId(tenantId)
            .setIfDescr(desc)
            .setIfIndex(1)
            .setIfAdminStatus(1)
            .setIfName(name)
            .setIfOperatorStatus(1)
            .setPhysicalAddr(physicalAddr)
            .setIfType(1)
            .setNodeLabel(nodelabel);
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        nodeServiceBlockingStub.createSnmpInterface(snmpCreateDTO.build());
    }

    @Then("fetch a list of snmp_interface by name with search {string}")
    public void fetchAListOfSnmpInterfaceByNameWithSearch(String search) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        SnmpInterfacesList list = nodeServiceBlockingStub.listSnmpInterfaces(SearchBy.newBuilder()
            .setSearch(search).build());
        list.getSnmpInterfacesList().stream().map(SnmpInterfaceDTO::getIfName)
            .forEach(label -> assertTrue(label.contains(search)));

    }
}

