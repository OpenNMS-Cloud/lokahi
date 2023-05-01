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

package org.opennms.horizon.systemtests;

import com.codeborne.selenide.Selenide;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import org.opennms.horizon.systemtests.api.portal.PortalApi;
import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;
import org.opennms.horizon.systemtests.pages.cloud.CloudLoginPage;
import org.opennms.horizon.systemtests.pages.portal.AddNewInstancePopup;
import org.opennms.horizon.systemtests.pages.portal.EditInstancePage;
import org.opennms.horizon.systemtests.pages.portal.PortalCloudPage;
import org.opennms.horizon.systemtests.pages.portal.PortalLoginPage;
import org.testcontainers.containers.GenericContainer;
import testcontainers.MinionContainer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CucumberHooks {
    public static final List<MinionContainer> MINIONS = new ArrayList<>();
    public static final List<String> INSTANCES = new ArrayList<>();
    public static String instanceUrl;
    public static String gatewayHost;
    private static String minionPrefix = "Default_Minion-";
    public static PortalApi portalApi = new PortalApi();

    @Before("@cloud")
    public static void setUp() {
        if (Selenide.webdriver().driver().hasWebDriverStarted()) {
            return;
        }

        Selenide.open(SecretsStorage.portalHost);
        PortalLoginPage.closeCookieHeader();
        PortalLoginPage.setUsername(SecretsStorage.adminUserEmail);
        PortalLoginPage.clickNext();
        PortalLoginPage.setPassword(SecretsStorage.adminUserPassword);
        PortalLoginPage.clickSignIn();

        PortalCloudPage.verifyMainPageHeader();

        long timeCode = Instant.now().toEpochMilli();

        String instanceName = "Cloud-env" + timeCode;
        PortalCloudPage.clickAddInstance();
        AddNewInstancePopup.setInstanceName(instanceName);
        AddNewInstancePopup.clickSubmitBtn();

        PortalCloudPage.mainPageIsNotCoveredByPopups();
        PortalCloudPage.setFilter(instanceName);
        PortalCloudPage.clickDetailsForFirstInstance();
        String instanceUrl = EditInstancePage.getInstanceUrl();
        CucumberHooks.instanceUrl = instanceUrl;
        CucumberHooks.gatewayHost = instanceUrl
            .replace("https://", "")
            .replace("tnnt", "minion");

        MinionContainer minionContainer = new MinionContainer(
            gatewayHost,
            minionPrefix + timeCode,
            "location-" + timeCode
        );

        minionContainer.start();
        MINIONS.add(minionContainer);

        EditInstancePage.clickOnInstanceUrl();

        CloudLoginPage.setUsername(SecretsStorage.adminUserEmail);
        CloudLoginPage.clickNextBtn();
        CloudLoginPage.setPassword(SecretsStorage.adminUserPassword);
        CloudLoginPage.clickSignInBtn();
    }

    @After("@cloud")
    public static void tearDownCloud() {
        Selenide.open(instanceUrl);

        Stream<MinionContainer> aDefault = MINIONS.stream().dropWhile(container -> !container.minionId.startsWith(minionPrefix));
        aDefault.forEach(GenericContainer::stop);

        if (MINIONS.isEmpty()) {
            long timeCode = Instant.now().toEpochMilli();
            MinionContainer.createNewOne(
                minionPrefix + timeCode,
                "location-" + timeCode
            );
        }
    }

    @Before("@portal")
    public static void loginToPortal() {
        portalApi.deleteAllBtoInstances();
        if (Selenide.webdriver().driver().hasWebDriverStarted()) {
            return;
        }
        Selenide.open(SecretsStorage.portalHost);
        PortalLoginPage.closeCookieHeader();
        PortalLoginPage.setUsername(SecretsStorage.adminUserEmail);
        PortalLoginPage.clickNext();
        PortalLoginPage.setPassword(SecretsStorage.adminUserPassword);
        PortalLoginPage.clickSignIn();

        PortalCloudPage.verifyMainPageHeader();
    }

    @After("@portal")
    public static void returnToPortalMainPage() {
        Selenide.open(SecretsStorage.portalHost + "/cloud");
        PortalCloudPage.verifyMainPageHeader();
        INSTANCES.clear();
    }

    @AfterAll
    public static void tearDown() {
        if (!MINIONS.isEmpty()) {
            MINIONS.get(0).stop();
        }
        portalApi.deleteAllBtoInstances();
    }
}
