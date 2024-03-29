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
package org.opennms.horizon.systemtests.pages;

import com.codeborne.selenide.*;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.opennms.horizon.systemtests.CucumberHooks;
import org.opennms.horizon.systemtests.steps.LocationSteps;
import org.opennms.horizon.systemtests.steps.MinionSteps;
import org.opennms.horizon.systemtests.utils.FileDownloadManager;
import org.openqa.selenium.By;
import testcontainers.MinionContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class WelcomePage {

    private static final SelenideElement downloadCertificateButton = $("[data-test='welcome-slide-two-download-button']");
    private static final SelenideElement dockerRunCLTextField = $(withText("GRPC_CLIENT_KEYSTORE_PASSWORD"));
    private static final SelenideElement minionStatusField = $("[data-test='welcome-minion-status-txt']");
    private static final SelenideElement continueButton = $("[data-id='welcome-slide-two-continue-button']");
    private static final SelenideElement discoveryIPField = $$("[data-test='welcome-slide-three-ip-input']").get(1);
    private static final SelenideElement startDiscoveryButton = $("[data-test='welcome-store-page-three-start-discovery-button']");
    private static final SelenideElement discoveryFinalContinueButton = $("[data-test='welcome-store-slide-three-continue-button']");

    private static final SelenideElement startSetupBtn = $(By.xpath("//button[@data-test='welcome-slide-one-setup-button']"));
    private static final SelenideElement downloadBundleBtn = $(By.xpath("//button[@data-test='welcome-slide-two-download-button']"));
    private static final SelenideElement dockerCmd = $(By.xpath("//div[@class='welcome-slide-table-body']/textarea"));
    private static final SelenideElement minionDetectedCheck = $(By.xpath("//div[text()='Minion detected.']"));
    private static final SelenideElement nodeDetectedCheck = $(By.xpath("//div[@data-test='item-preview-status-id'][text()='UP']"));
    private static final SelenideElement discoveryResultLatencyCheck = $(By.xpath("//div[@data-test='item-preview-status-id'][.<=800]"));
    private static final SelenideElement INITIAL_PAGE_CHECK = $(By.xpath("//button[@data-test='welcome-slide-one-setup-button']|//div[@class='app-aside']"));

    public static void checkIsStartSetupButtonVisible() {
        startSetupBtn.shouldBe(enabled);
    }

    @SneakyThrows
    public static void startWelcomeWizardSetup() {
        startSetupBtn.shouldBe(enabled).click();
    }

    public static void checkMinionConnection() {
        for (int i = 0; i < 120; i++) {
            Selenide.sleep(5000);
            // TODO: Should be moved to properties value.

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
        nodeDetectedCheck.should(exist, Duration.ofMinutes(3));
        discoveryResultLatencyCheck.should(exist);
    }

    public static void clickContinueToEndWizard() {
        discoveryFinalContinueButton.shouldBe(enabled).click();
    }

    public static boolean containsWalkthroughButton() {
        return startSetupBtn.exists();
    }

    public static void waitOnWalkthroughOrMain() {
        INITIAL_PAGE_CHECK.should(exist);
    }

    public static MinionContainer addMinionUsingWalkthrough(String minionName) {
        try {
            File bundle = FileDownloadManager.downloadCertificate(downloadBundleBtn);
            if (bundle != null) {
                String key = readCertKey(bundle);

                MinionContainer minion = MinionSteps.startMinion(bundle, key, minionName, LocationSteps.DEFAULT_LOCATION_NAME);
                // Minion startup and connect is slow - need a specific timeout here
                minionDetectedCheck.should(exist, Duration.ofSeconds(120));
                return minion;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failure downloading certificate bundle file");
    }

    public static String readCertKey(File zipBundle) throws IOException {
        ZipInputStream zstream = new ZipInputStream(Files.newInputStream(zipBundle.toPath()));
        ZipEntry zentry = zstream.getNextEntry();
        while (zstream.available() != 0 && zentry != null && !zentry.getName().equals("docker-compose.yaml")) {
            zstream.closeEntry();
            zentry = zstream.getNextEntry();
        }
        if (zentry == null || !zentry.getName().equals("docker-compose.yaml")) {
            throw new IOException("Unable to locate certificate key in zip file");
        }

        byte[] byteData = ByteStreams.toByteArray(zstream);
        String dockerString = new String(byteData, StandardCharsets.UTF_8);

        Pattern pattern = Pattern.compile("GRPC_CLIENT_KEYSTORE_PASSWORD: \"([a-z,0-9,-]*)\"");
        Matcher matcher = pattern.matcher(dockerString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Unable to parse cert password from docker file - " + dockerString);
    }

    public static void startSetup() {
        startSetupBtn.shouldBe(enabled).click();
    }
}
