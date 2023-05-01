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

import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class DeleteInstancePopup {

    private static final SelenideElement popup = $("[data-ref-id='feather-dialog']");
    private final static SelenideElement closeBtn = $("[data-ref-id='dialog-close']");
    private final static SelenideElement confirmNameInp = $("#delete-instance-confirm-input");
    private final static SelenideElement cancelBtn = $("#cloud-delete-instance-cancel");
    private final static SelenideElement deleteBtn = $("#cloud-delete-instance-confirm");
    private final static SelenideElement errorTxt = $("div#cloud-instance-delete-input-wrapper [data-ref-id='feather-form-element-error']");

    public static void waitPopupIsDisplayed(boolean isVisible) {
        if (isVisible) {
            popup.shouldBe(Condition.visible);
        } else {
            popup.shouldBe(Condition.disappear);
        }
    }

    public static void clickOnCloseBtn() {
        closeBtn.shouldBe(enabled).click();
    }

    public static void clickOnCancelBtn() {
        cancelBtn.shouldBe(enabled).click();
    }

    public static void clickOnDeleteBtn() {
        deleteBtn.shouldBe(enabled).click();
    }

    public static void setInstanceNameToConfirmationInput(String instanceName) {
        confirmNameInp.shouldBe(enabled).setValue("").sendKeys(instanceName);
    }

    public static void verifyErrorText(String errorMessage) {
        errorTxt.shouldBe(visible).shouldHave(exactText(errorMessage));
    }

    public static void verifyNoError() {
        errorTxt.shouldBe(disappear);
    }

    public static void closePopup() {
        closeBtn.shouldHave(enabled).click();
    }
}
