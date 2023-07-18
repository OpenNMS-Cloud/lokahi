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
package org.opennms.horizon.systemtests.pages.cloud;

import com.codeborne.selenide.*;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.opennms.horizon.systemtests.utils.MinionStarter;
import org.openqa.selenium.NoSuchElementException;
import testcontainers.DockerComposeMinionContainer;
import java.io.File;
import java.time.Duration;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class WelcomePage {

    private static final SelenideElement startSetupButton = $("[data-test='welcome-slide-one-setup-button']");
    private static final SelenideElement downloadCertificateButton = $("[data-test='welcome-slide-two-download-button']");
    private static final SelenideElement dockerRunCLTextField = $(withText("GRPC_CLIENT_KEYSTORE_PASSWORD"));
    private static final SelenideElement minionStatusField = $("[data-test='welcome-minion-status-txt']");
    private static final SelenideElement continueButton = $("[data-id='welcome-slide-two-continue-button']");
    private static final SelenideElement discoveryIPField = $$("[data-test='welcome-slide-three-ip-input']").get(1);
    private static final SelenideElement startDiscoveryButton = $("[data-test='welcome-store-page-three-start-discovery-button']");
    private static final SelenideElement discoveryLoadingField = $("[data-test='welcome-discovery-status-txt']");
    private static final SelenideElement discoveryResultNodeNameField = $("[data-test='item-preview-meta-id']");
    private static final ElementsCollection discoveryResultNodeStatusFields = $$("[data-test='item-preview-status-id']");
    private static final SelenideElement discoveryFinalContinueButton = $("[data-test='welcome-store-slide-three-continue-button']");
    public static void checkIsStartSetupButtonVisible() {
        startSetupButton.shouldBe(enabled);
    }

    @SneakyThrows
    public static void startWelcomeWizardSetup() {
        startSetupButton.shouldBe(enabled).click();
    }

    @SneakyThrows
    public static void downloadCertificateAndStartMinion(String minionID, String dockerComposeFile) {
        MinionStarter.downloadCertificateAndStartMinion(minionID, dockerComposeFile, downloadCertificateButton, dockerRunCLTextField);
    }

    public static void checkMinionConnection() {
        for (int i = 0; i < 10; i++) {
            Selenide.sleep(5000);
            if (!"Please wait while we detect your Minion.".equals(minionStatusField.getText())) {
                break;
            }
        }
        Assert.assertEquals("Expected to see 'Minion detected.'", "Minion detected.", minionStatusField.getText());
    }

    public static void continueToDiscovery() {
        continueButton.shouldBe(enabled).click();
    }

    public static void setIPForDiscovery(String ip) {
        discoveryIPField.shouldBe(enabled).val("");
        discoveryIPField.sendKeys(ip);
    }

    public static void clickStartDiscovery() {
        startDiscoveryButton.shouldBe(enabled).click();
    }

    public static void nodeDiscovered(String sysName) {
        try {
            for (int i = 0; i < 50; i++) {
                Selenide.sleep(3000);
                if (!"Loading first discovery.".equals(discoveryLoadingField.getText())) {
                    break;
                }
            }
        }
        catch (com.codeborne.selenide.ex.ElementNotFound e) {
            Assert.assertEquals(sysName, discoveryResultNodeNameField.getText());
            Assert.assertEquals("Minion status should be 'UP'", "UP", discoveryResultNodeStatusFields.get(0).getText());
            Assert.assertTrue("ICMP should be less then 500", 500 >= Double.valueOf(discoveryResultNodeStatusFields.get(1).getText()));
        }
    }

    public static void clickContinueToEndWizard() {
        discoveryFinalContinueButton.shouldBe(enabled).click();
    }

    public static void waitPageLoaded() {
        startSetupButton.shouldBe(visible, Duration.ofSeconds(5));
    }
}
