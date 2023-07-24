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
package org.opennms.horizon.systemtests.utils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.SneakyThrows;
import org.junit.Assert;
import testcontainers.DockerComposeMinionContainer;

import java.io.File;

import static com.codeborne.selenide.Condition.enabled;

public class MinionStarter {

    @SneakyThrows
    public static void downloadCertificateAndStartMinion(String minionID, String dockerComposeFile, SelenideElement downloadCertificateButton, SelenideElement dockerRunCLTextField) {
        Configuration.fileDownload = FileDownloadMode.FOLDER;
        File cert = downloadCertificateButton.shouldBe(enabled).download(60000);
        Assert.assertTrue(cert.exists());

        String dockerCL = null;
        for (int i = 0; i < 10; i++) {
            Selenide.sleep(3000);
            dockerCL = dockerRunCLTextField.getText();
            if (dockerCL.indexOf("GRPC_CLIENT_KEYSTORE_PASSWORD='") > 0) {
                break;
            }
        }

        int start = dockerCL.indexOf("GRPC_CLIENT_KEYSTORE_PASSWORD='") + "GRPC_CLIENT_KEYSTORE_PASSWORD='".length();
        int end = dockerCL.indexOf("' -e MINION_ID=");
        Assert.assertTrue (end > start && start > 0);

        String password = dockerCL.substring(start, end);
        DockerComposeMinionContainer.createNewContainer("src/test/resources/" + dockerComposeFile, cert, minionID, password);
    }
}
