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
import io.cucumber.java.en.Then;
import org.junit.Assert;
import org.opennms.horizon.systemtests.CucumberHooks;
import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;
import org.opennms.horizon.systemtests.pages.cloud.CloudLoginPage;
import org.opennms.horizon.systemtests.utils.TestDataStorage;

public class CloudLoginSteps {

    @Then("Cloud login page appears")
    public void checkPopupIsVisible() {
        CloudLoginPage.checkPageTitle();
    }

    @Then("set email address as {string}")
    public void setEmail(String email) {
        String userEmail = TestDataStorage.mapUserToEmail(email);
        CloudLoginPage.setUsername(userEmail);
    }

    @Then("click on 'Next' button")
    public void clickNextBtn() {
        CloudLoginPage.clickNextBtn();
    }

    @Then("set password")
    public void setPassword() {
        CloudLoginPage.setPassword(SecretsStorage.adminUserPassword);
    }

    @Then("click on 'Sign in' button")
    public void clickSignIn() {
        CloudLoginPage.clickSignInBtn();
    }

    @Then("verify the instance url for {string} instance")
    public void checkInstanceUrl(String instanceName) {
        String expectedUrl = CucumberHooks.portalApi.getAllBtoInstancesByName(instanceName).pagedRecords.get(0).url;
        String actualUrl = Selenide.webdriver().driver().url();

        Assert.assertTrue(
            String.format("Expected url:\n%s\nactual url\n%s", expectedUrl, actualUrl),
            actualUrl.replace("https://", "").startsWith(expectedUrl));
    }
}

