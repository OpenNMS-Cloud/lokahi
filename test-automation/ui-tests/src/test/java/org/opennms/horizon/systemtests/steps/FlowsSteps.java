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
package org.opennms.horizon.systemtests.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.systemtests.pages.FlowsPage;
import org.opennms.horizon.systemtests.pages.DiscoveryPage;
import org.opennms.horizon.systemtests.pages.LocationsPage;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;


public class FlowsSteps {
    @Then("sees 'No Data' in the flows table")
    public void verifyNoData() {
        FlowsPage.verifyNoDataTitle();
    }

    @Then("sees chart with new netflow data")
    public void verifyChartDataUpdates() {
        FlowsPage.verifyTopApplicationFlowTotalsIncreases();
    }

    @Then("click on 'Exporter' filter")
    public void clickOn() {
        FlowsPage.clickOnExporterInput();
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

    @Then("add SNMP discovery {string} for location {string} with node {string}")
    public void addDiscoveryForLocation(String discoveryName, String locationName, String nodeName) {
        DiscoveryPage.clickAddDiscovery();
        DiscoveryPage.selectICMP_SNMP();
        DiscoveryPage.createNewSnmpDiscovery(discoveryName, locationName, DiscoverySteps.getIpaddress(nodeName));
    }

    @And("check if exporter {string} visible in the dropdown")
    public void checkIfExporterVisibleInTheDropdown(String exporterName) {
        FlowsPage.checkExporterDropdown(exporterName);
    }

    @When("Send flow data for node {string}")
    public void sendFlowDataForNode(String node) throws IOException, InterruptedException {
        GenericContainer nodeContainer = DiscoverySteps.getNode(node);
        String minionIP = MinionSteps.getMinionIp();

        Thread thread = new Thread() {
            public void run() {
                try {
                    nodeContainer.execInContainer("/udpgen", "-x", "netflow9", "-i", "-h", minionIP, "-p", "4729", "-r", "1");
                } catch (Exception e) {
                    throw new RuntimeException("Connector error");
                }
            }
        };
        thread.start();
    }
}
