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
    public static String instanceUrl;
    public static String gatewayHost;
    private static String minionPrefix = "Default_Minion-";

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

        PortalCloudPage.verifyThatUserLoggedIn();

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
        CloudLoginPage.clickSubmitBtn();
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
        if (Selenide.webdriver().driver().hasWebDriverStarted()) {
            return;
        }
        Selenide.open(SecretsStorage.portalHost);
        PortalLoginPage.closeCookieHeader();
        PortalLoginPage.setUsername(SecretsStorage.adminUserEmail);
        PortalLoginPage.clickNext();
        PortalLoginPage.setPassword(SecretsStorage.adminUserPassword);
        PortalLoginPage.clickSignIn();

        PortalCloudPage.verifyThatUserLoggedIn();
    }

    @After("@portal")
    public static void returnToPortalMainPage() {
        Selenide.open(SecretsStorage.portalHost + "/cloud");
    }

    @AfterAll
    public static void tearDown() {
        if (!MINIONS.isEmpty()) {
            MINIONS.get(0).stop();
        }
        PortalApi portalApi = new PortalApi();
        portalApi.deleteAllBtoInstances();

    }
}
