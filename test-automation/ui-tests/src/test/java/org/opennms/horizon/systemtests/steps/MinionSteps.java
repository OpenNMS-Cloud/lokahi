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

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.LeftPanelPage;
import org.opennms.horizon.systemtests.pages.LocationsPage;
import org.openqa.selenium.By;
import org.testcontainers.containers.Network;
import testcontainers.MinionContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class MinionSteps {
    private static final ElementsCollection locationMenus = $$(By.xpath("//div[@class='locations-card']//button[@data-test='more-options-btn']"));
    private static final SelenideElement firstMenu = $(By.xpath("//div[@class='locations-card']//button[@data-test='more-options-btn'][1]"));

    private static Map<String, MinionContainer> minions = new HashMap<>();


    public static void waitForMinionUp(String minionName) {
        SelenideElement minionStatus = $(By.xpath("//ul[@class='minions-list']/li[.//div[@data-test='header-name']/text()='" + minionName.toUpperCase() + "']//div[@class='status']//span[translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='up']"));

        // Make sure we're on the locations page with the right location selected
        LeftPanelPage.clickOnPanelSection("locations");
        LocationsPage.selectLocation(LocationSteps.getLocationName());

        // The UI can take a very long time to reflect minion status updates. Might need to force refreshed to speed this up.
        minionStatus.should(exist, Duration.ofSeconds(120));
    }

    public static void addMinionForLocation(String minionName) {
        // Ensure the location exists
        LocationSteps.addOrEditLocation();
        MinionContainer minion = LocationSteps.addMinionFromLocationPane(minionName);

        minions.put(LocationSteps.getLocationName(), minion);
        waitForMinionUp(minionName);
    }

    public static MinionContainer startMinion(File bundle, String pwd, String minionId, String locationName) throws IOException {
        Network network = SetupSteps.getCommonNetwork();

        File cert = File.createTempFile("minionCert", "p12");
        extractCert(bundle, cert);
        MinionContainer minion = new MinionContainer(minionId, "minion-" + minionId.toLowerCase(), network,
            cert, pwd);

        minion.start();
        minions.put(locationName, minion);
        return minion;
    }

    private static void extractCert(File zip, File cert) throws IOException {
        ZipInputStream zstream = new ZipInputStream(Files.newInputStream(zip.toPath()));
        ZipEntry zentry = zstream.getNextEntry();
        while (zstream.available() != 0 && zentry != null && !zentry.getName().contains(".p12")) {
            zstream.closeEntry();
            zentry = zstream.getNextEntry();
        }
        if (zentry == null || !zentry.getName().contains(".p12")) {
            throw new IOException("Unable to locate .p12 certificate file in zip");
        }

        OutputStream ostream = new FileOutputStream(cert);
        byte[] buffer = new byte[4096];
        int readBytes;
        while ((readBytes = zstream.read(buffer)) != -1) {
            ostream.write(buffer, 0, readBytes);
        }
        ostream.close();
    }

    @Then("stop minion for location {string}")
    public void stopMinionForLocation(String locationName) {
        minions.get(locationName).stop();
        for (int i = 0; i < 30; i++) {
            if (minions.get(locationName).isMinionIsStopped) {
                break;
            }
            Selenide.sleep(2000);
        }
    }

    public static boolean isMinionRunning(String locationName) {
        MinionContainer minionContainer = minions.get(locationName);
        boolean result = false;
        if (minionContainer != null) {
            result = minionContainer.isRunning();
        }
        return result;
    }

    public static String getMinionIp() {
        MinionContainer minionContainer = minions.get(LocationSteps.getLocationName());
        return DiscoverySteps.getContainerIP(minionContainer);
    }
}
