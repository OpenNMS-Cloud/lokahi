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
import org.opennms.horizon.systemtests.pages.cloud.CloudLoginPage;
import org.opennms.horizon.systemtests.pages.cloud.CloudInstanceLoginPage;
import org.opennms.horizon.systemtests.pages.cloud.WelcomePage;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import testcontainers.DockerComposeMinionContainer;
import testcontainers.MinionContainer;
import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CucumberHooks {
    public static final List<MinionContainer> MINIONS = new ArrayList<>();
    public static final Map<String, DockerComposeMinionContainer> DC_MINIONS = new HashMap<>();
    public static String instanceUrl;
    private static final String MINION_PREFIX = "Default_Minion-";
    public static String gatewayPort;
    public static String gatewayUrl;

    private static final String LOCAL_INSTANCE_URL_DEFAULT = "https://onmshs.local:1443";
    private static final String LOCAL_MINION_GATEWAY_HOST_DEFAULT = "minion.onmshs.local";
    private static final String LOCAL_MINION_GATEWAY_HOST_PORT = "1443";
    private static final String ADMIN_DEFAULT_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin";
    private static final String CLOUD_MINION_GATEWAY_HOST_PORT = "443";
    private static final String CLOUD_MINION_GATEWAY_HOST = "CLOUD_MINION_GATEWAY_HOST";
    // for example: https://2e6975ac-12c0-47db-9213-3975e8dc6092.tnnt.onms-fb-dev.dev.nonprod.dataservice.opennms.com
    private static final String CLOUD_INSTANCE_URL = "CLOUD_INSTANCE_URL";
    private static final String CLOUD_USERNAME = "CLOUD_USERNAME";
    private static final String CLOUD_PASSWORD = "CLOUD_PASSWORD";
    private static final String IS_CLOUD_TESTING_TARGET = "IS_CLOUD_TESTING_TARGET";
    private static boolean cloudEnv;

    private static void setUp() {
        if (Selenide.webdriver().driver().hasWebDriverStarted()) {
            return;
        }

        String is_cloud_testing_target = System.getenv().get(IS_CLOUD_TESTING_TARGET);
        if (is_cloud_testing_target != null) {
            cloudEnv = Boolean.valueOf(is_cloud_testing_target);
        }

        if (cloudEnv) {
            instanceUrl = System.getenv().get(CLOUD_INSTANCE_URL);
            gatewayPort = CLOUD_MINION_GATEWAY_HOST_PORT;
            gatewayUrl = System.getenv().get(CLOUD_MINION_GATEWAY_HOST);
            Selenide.open(instanceUrl);
            loginToCloudInstance();
        } else {
            instanceUrl = LOCAL_INSTANCE_URL_DEFAULT;
            gatewayPort = LOCAL_MINION_GATEWAY_HOST_PORT;
            gatewayUrl = LOCAL_MINION_GATEWAY_HOST_DEFAULT;
            Selenide.open(instanceUrl);
            loginToLocalInstance();
        }
    }

    @Before("@cloud_welcome")
    public static void setupWithEnabledWelcomeWizard() {
        setUp();
    }

    @Before("@cloud")
    public static void setupWithDisabledWelcomeWizard() {
        setUp();
        try {
            WelcomePage.waitPageLoaded();
            Selenide.sessionStorage().setItem("welcomeOverride", "true");
            Selenide.open(instanceUrl);
        } catch (com.codeborne.selenide.ex.ElementNotFound e) {
            // in case there is no welcome page after login, do nothing and continue
        }
    }

    private static void loginToLocalInstance() {
        CloudLoginPage.checkPageTitle();
        CloudLoginPage.setUsername(ADMIN_DEFAULT_USERNAME);
        CloudLoginPage.setPassword(ADMIN_DEFAULT_PASSWORD);
        CloudLoginPage.clickSignInBtn();
    }

    private static void loginToCloudInstance() {
        CloudInstanceLoginPage.setUsername(System.getenv().get(CLOUD_USERNAME));
        CloudInstanceLoginPage.clickNextBtn();
        CloudInstanceLoginPage.setPassword(System.getenv().get(CLOUD_PASSWORD));
        CloudInstanceLoginPage.clickSubmitBtn();
    }

    @After("@cloud or @cloud_welcome")
    public static void tearDownCloud() {
        Selenide.open(instanceUrl);

        Stream<MinionContainer> aDefault = MINIONS.stream();
        aDefault.forEach(GenericContainer::stop);

        if (MINIONS.isEmpty()) {
            long timeCode = Instant.now().toEpochMilli();
            MinionContainer.createNewOne(
                MINION_PREFIX + timeCode,
                "location-" + timeCode
            );
        }
    }

    @AfterAll
    public static void tearDown() {
        Stream<MinionContainer> aDefault = MINIONS.stream();
        aDefault.forEach(GenericContainer::stop);

        Stream<DockerComposeMinionContainer> bDefault = DC_MINIONS.values().stream();
        bDefault.forEach(DockerComposeContainer::stop);

        File download = Paths.get("target/downloads").toFile();
        download.delete();
    }
}
