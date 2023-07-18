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
package org.opennms.horizon.systemtests.steps.cloud;

import com.codeborne.selenide.Selenide;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.junit.Assert;
import org.opennms.horizon.systemtests.pages.cloud.CloudFlowsPage;
import org.opennms.horizon.systemtests.pages.cloud.DiscoveryPage;
import org.opennms.horizon.systemtests.pages.cloud.InventoryPage;
import org.opennms.horizon.systemtests.pages.cloud.LocationsPage;


public class CloudFlowsSteps {
    @Then("sees 'No Data' in the flows table")
    public void verifyNoData() {
        CloudFlowsPage.verifyNoDataTitle();
    }

    @Then("sees chart for netflow data")
    public void verifyChartVisibility() {
        CloudFlowsPage.verifyChartVisibility();
    }

    @Then("click on 'Exporter' filter")
    public void clickOn() {
        CloudFlowsPage.clickOnExporterInput();
    }

    @Given("No netflow data was sent")
    public void noNetflowDataWasSent() {

    }

    @Given("location {string} created")
    public void locationCreated(String locationName) {
        LocationsPage.addNewLocation(locationName);
    }

    @Given("minion for location {string} started that generates SNMP and Flow data")
    public void minionForLocationStartedThatGeneratesSNMPAndFlowData(String locationName) {
    }

    @Then("click on 'Download Certificate' button, get certificate and password for minion {string} and start minion using {string}")
    public void clickOnDownloadButtonGetCertificateAndPasswordForMinionAndStartMinionUsing(String minionId, String dockerComposeFile) {
        LocationsPage.downloadCertificateAndStartMinion(minionId, dockerComposeFile);
    }

    @Then("add SNMP discovery {string} for location {string} with IP {string}")
    public void addDiscoveryForLocation(String discoveryName, String locationName, String ip) {
        DiscoveryPage.clickAddDiscovery();
        DiscoveryPage.selectICMP_SNMP();
        DiscoveryPage.createNewSnmpDiscovery(discoveryName, locationName, ip);
    }

    @Then("check node with system name {string} discovered successfully")
    public void checkNodeWithSystemNameDiscoveredSuccessfully(String nodeSysName) {
        InventoryPage.clickOnMonitoredNodesTab();
        for (int i = 0; i < 20; i++) {
            Selenide.sleep(3_000);
            Selenide.refresh();
            if (InventoryPage.checkIfMonitoredNodeExist(nodeSysName)) {
                break;
            }
        }
        Assert.assertTrue(InventoryPage.checkIfMonitoredNodeExist(nodeSysName));
    }

    @And("check if exporter {string} visible in the dropdown")
    public void checkIfExporterVisibleInTheDropdown(String exporterName) {
        CloudFlowsPage.checkDropdown(exporterName);
    }
}
