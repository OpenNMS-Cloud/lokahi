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


package org.opennms.horizon.systemtests.pages.portal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class AddUserPopup {
    private static final SelenideElement popup = $("[data-ref-id='feather-dialog']");
    private static final SelenideElement emailAddressInp = $("textarea#cloud-add-user-dialog-email-with");
    private static final SelenideElement emailAddressDropdown = $("div.feather-menu-dropdown .feather-list-item-text");
    private static final SelenideElement emailAddressErrorTxt = $("#cloud-add-user-dialog-email-with .feather-input-error");
    private static final SelenideElement addBtn = $(By.id("cloud-add-user-dialog-confirm"));
    private static final SelenideElement cancelBtn = $(By.id("cloud-add-user-dialog-cancel"));
    private static final SelenideElement closeBtn = $("[data-ref-id='dialog-close']");

    public static void popupIsVisible(boolean state) {
        popup.shouldBe(state ? visible : hidden);
    }

    public static void clickCloseBtn() {
        closeBtn.shouldBe(enabled).click();
    }

    public static void setEmailAddress(String email) {
        emailAddressInp.setValue("").sendKeys(email);
    }

    public static void confirmEmailInDropdown(String email) {
        emailAddressDropdown.shouldHave(Condition.text(email), Condition.visible).click();
    }

    public static void verifyErrorMessageForEmailAddress(String errorMessage) {
        emailAddressErrorTxt.shouldHave(Condition.text(errorMessage));
    }

    public static void verifyNoErrorMessageForEmailAddress() {
        emailAddressErrorTxt.shouldBe(Condition.hidden);
    }

    public static void clickAddBtn() {
        addBtn.shouldBe(enabled).click();
    }

    public static void clickCancelBtn() {
        cancelBtn.shouldBe(enabled).click();
    }
}
