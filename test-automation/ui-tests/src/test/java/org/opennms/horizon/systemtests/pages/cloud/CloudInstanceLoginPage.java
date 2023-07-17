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

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Selenide.$;

public class CloudInstanceLoginPage {
    private static final SelenideElement usernameField = $(By.id("idp-discovery-username"));
    private static final SelenideElement nextBtn = $(By.id("idp-discovery-submit"));
    private static final SelenideElement passwordField = $(By.id("okta-signin-password"));
    private static final SelenideElement submitBtn = $(By.id("okta-signin-submit"));

    public static void setUsername(String username) {
        usernameField.shouldBe(enabled).setValue(username);
    }

    public static void clickNextBtn() {
        nextBtn.shouldBe(enabled).click();
    }

    public static void setPassword(String password) {
        passwordField.shouldBe(enabled).setValue(password);
    }

    public static void clickSubmitBtn() {
        submitBtn.shouldBe(enabled).click();
    }
}
