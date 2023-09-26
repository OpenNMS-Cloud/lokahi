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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import java.io.File;
import java.io.FileNotFoundException;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;

public class FileDownloadManager {

    public static File downloadCertificate(SelenideElement downloadBundleBtn) throws FileNotFoundException {
        File bundle = null;
        for (int i = 0; i < 5; i++) {
            bundle = downloadBundleBtn.should(exist).shouldBe(enabled).download(60000);
            if (bundle.exists() && bundle.canRead() && bundle.length() > 0 && bundle.getName().endsWith(".zip")) {
                break;
            } else {
                bundle = null;
                Selenide.sleep(2000);
            }
        }
        return bundle;
    }
}
