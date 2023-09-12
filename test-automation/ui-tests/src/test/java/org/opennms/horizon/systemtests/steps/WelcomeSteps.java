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

import com.codeborne.selenide.Selenide;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.CucumberHooks;
import org.opennms.horizon.systemtests.pages.LocationsPage;
import org.opennms.horizon.systemtests.pages.WelcomePage;



public class WelcomeSteps {
    @Given("Open the welcome wizard")
    public void loggedInAtWelcomeWizard() {
        // Force going to the welcome wizard just in case
        Selenide.open(CucumberHooks.instanceUrl + "/welcome");
    }

    @Given("check 'Start Setup' button is accessible and visible")
    public static void checkStartSetupButtonIsAccessibleAndVisible() {
        WelcomePage.checkIsStartSetupButtonVisible();
    }

    @Then("click on 'Start Setup' button to start welcome wizard")
    public static void startWelcomeWizardSetup() {
        WelcomePage.startWelcomeWizardSetup();
    }

    @Then("click on 'Download' button to get certificate and password for minion {string}")
    public static void downloadCertificate(String minionID) {
        WelcomePage.addMinionUsingWalkthrough(minionID);
    }

    @Then("wizard shows that minion connected successfully")
    public static void minionStarted() {
        WelcomePage.checkMinionConnection();
    }

    @Then("click on 'Continue' button")
    public static void continueToDiscovery() {
        WelcomePage.continueToDiscovery();
    }

    @Then("enter IP of {string} for discovery")
    public static void continueToDiscovery(String nodeName) {
        WelcomePage.setIPForDiscovery(DiscoverySteps.getIpaddress(nodeName));
    }

    @Then("click on 'Start Discovery' button")
    public static void clickStartDiscovery() {
        WelcomePage.clickStartDiscovery();
    }

    @Then("first node with system name {string} discovered successfully")
    public static void nodeDiscovered(String sysName) {
        WelcomePage.nodeDiscovered(sysName);
    }

    @Then("click on 'Continue' button to end the wizard")
    public static void clickContinueToEndWizard() {
        WelcomePage.clickContinueToEndWizard();
    }

    @Then("check {string} location exists")
    public void checkLocationExists(String locationName) {
        LocationsPage.checkLocationExists(locationName);
    }

    @Then("click on location {string}")
    public void clickOnLocation(String locationName) {
        LocationsPage.clickOnLocation(locationName);
    }

    @Then("check minion {string} exists")
    public void checkMinionExists(String minionId) {
        LocationsPage.checkMinionExists(minionId);
    }

    @Then("delete minion {string}")
    public void deleteMinion(String minionId) {
        LocationsPage.deleteMinion(minionId);
    }

    @Then("verify minion {string} deleted")
    public void verifyMinionDeleted(String minionId) {
        LocationsPage.checkMinionDoesntExist(minionId);
    }

    @Given("skip welcome wizard")
    public void skipWelcomWizard() {
        Selenide.localStorage().setItem("welcomeOverride", "true");
    }
}
