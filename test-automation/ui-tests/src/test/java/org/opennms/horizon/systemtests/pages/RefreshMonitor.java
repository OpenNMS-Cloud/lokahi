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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

public class RefreshMonitor {
    public static void waitForElement(SelenideElement element, Condition condition, int totalWaitTime, boolean isPositiveCheck) {
        int individualWaitTime=5;
        if (totalWaitTime <= 0) {
            throw new RuntimeException("Invalid wait time of " + totalWaitTime);
        }

        // Need to have the duration/wait as part of the refresh since redrawing everything can take some time
        // The duration isn't supported as part of the regular 'find' methods and only the 'should' methods
        while (totalWaitTime > 0) {
            try {
                if (isPositiveCheck) {
                    element.shouldBe(condition, Duration.ofSeconds(individualWaitTime));
                } else {
                    // Negative check may need to wait for the refresh to complete, so wait separately then check
                    // if the element is showing up
                    Selenide.sleep(individualWaitTime * 1000);
                    element.shouldNotBe(condition, Duration.ofSeconds(0));
                }
                return;
            } catch (com.codeborne.selenide.ex.ElementNotFound | com.codeborne.selenide.ex.ElementShouldNot e) {
                totalWaitTime -= individualWaitTime;
                Selenide.refresh();
            }
        }

    }
}
