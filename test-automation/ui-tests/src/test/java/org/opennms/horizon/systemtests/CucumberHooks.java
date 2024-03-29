/*******************************************************************************
 * This file is part of OpenNMS(R).
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.systemtests;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.codeborne.selenide.Selenide;
import io.cucumber.java.*;
import org.opennms.horizon.systemtests.steps.DiscoverySteps;
import org.opennms.horizon.systemtests.steps.LocationSteps;
import org.opennms.horizon.systemtests.steps.MinionSteps;
import org.opennms.horizon.systemtests.steps.SetupSteps;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.GenericContainer;
import testcontainers.MinionContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CucumberHooks {
    public static final Map<String, MinionContainer> MINIONS = new HashMap<>(); // Location name -> Minion

    private static final String KEYCLOAK_LOGIN = "KEYCLOAK_LOGIN";
    public static String instanceUrl;
    public static String gatewayHost;
    public static String gatewayPort;
    private static final String MINION_PREFIX = "Default_Minion-";
    private static final String LOCAL_MINION_GATEWAY_HOST_DEFAULT = "minion.onmshs.local";


    private static final String ADMIN_DEFAULT_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin";
    public static String overrideAuthority;

    public static String adminUsername;
    public static String adminPassword;

    public static String minionDockerTag = "latest";
    public static boolean keycloakLogin = true;

    public static String defaultMinionName = "testMinion";

    @BeforeAll
    public static void setUpForAllTests() {
        Configuration.fileDownload = FileDownloadMode.FOLDER;
        Configuration.headless = true;
        Configuration.timeout = 20000;
        Configuration.reopenBrowserOnFail = true;

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-background-networking");
        options.addArguments("--enable-features=NetworkService,NetworkServiceInProcess");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-breakpad");
        options.addArguments("--disable-client-side-phishing-detection");
        options.addArguments("--disable-component-extensions-with-background-pages");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-features=TranslateUI,ChromePDF");
        options.addArguments("--disable-hang-monitor");
        options.addArguments("--disable-ipc-flooding-protection");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-prompt-on-repost");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-sync");
        options.addArguments("--force-color-profile=srgb");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--no-first-run");
        options.addArguments("--password-store=basic");
        options.addArguments("--use-mock-keychain");
        options.addArguments("--hide-scrollbars");
        options.addArguments("--mute-audio");

        Configuration.browserCapabilities = options;

        String keycloak = System.getenv().get(KEYCLOAK_LOGIN);
        if (keycloak != null) {
            keycloakLogin = Boolean.parseBoolean(keycloak);
        }

        adminUsername = System.getenv("KEYCLOAK_USERNAME");
        if (adminUsername == null) {
            adminUsername = ADMIN_DEFAULT_USERNAME;
        }
        adminPassword = System.getenv("KEYCLOAK_PASSWORD");
        if (adminPassword == null) {
            adminPassword = ADMIN_DEFAULT_PASSWORD;
        }

        gatewayHost = System.getenv("MINION_INGRESS");
        if (gatewayHost == null) {
            gatewayHost = LOCAL_MINION_GATEWAY_HOST_DEFAULT;
        }

        gatewayPort = System.getenv("MINION_INGRESS_PORT");
        if (gatewayPort == null) {
            gatewayPort = "1443";
        }

        overrideAuthority = System.getenv("MINION_INGRESS_OVERRIDE_AUTHORITY");
        instanceUrl = System.getenv("INGRESS_BASE_URL");
        if (instanceUrl == null) {
            instanceUrl = "https://onmshs.local:" + gatewayPort;
        }

        minionDockerTag = System.getenv("MINION_DOCKER_TAG");
        if (minionDockerTag == null) {
            minionDockerTag = "latest";
        }

        SetupSteps.login();
    }

    @Before
    public static void setUp(Scenario scenario) {
        // we will start minion only once per location and also never for welcome test
        if ((!scenario.getSourceTagNames().contains("@welcome")) && !MinionSteps.isMinionRunning(LocationSteps.getLocationName())) {
            SetupSteps.startNamedMinion(defaultMinionName);
        }
    }

    @After
    public static void cleanupIndividualTests() {
        DiscoverySteps.cleanup();
    }

    @AfterAll
    public static void tearDown() {
        Stream<MinionContainer> aDefault = MINIONS.values().stream().dropWhile(container -> !container.minionId.startsWith(MINION_PREFIX));

        aDefault.forEach(GenericContainer::stop);

        // Give a couple seconds as sometimes the UI cleanup calls are still in progress in the background
        Selenide.sleep(2000);
    }
}
