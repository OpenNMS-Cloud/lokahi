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
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class EditInstanceNamePopup {
    private final static SelenideElement popup = $("[data-ref-id='feather-dialog']");
    private final static SelenideElement nameInp = $("#edit-instance-name");
    private final static SelenideElement updateBtn = $("#submit-cloud\\ instance");
    private final static SelenideElement cancelBtn = $("#cancel-btn-cloud\\ instance");
    private final static SelenideElement closeBtn = $("[data-ref-id='dialog-close']");
    private final static SelenideElement errorTxt = $("div.cloud-instance-edit-dialog [data-ref-id='feather-form-element-error']");
    private final static SelenideElement urlTxt = $("#cloud-details-instance-url");

    public static void waitNewState(Condition condition) {
        popup.shouldBe(condition);
    }

    public static void setNewInstanceName(String instanceName) {
        nameInp.shouldBe(enabled).setValue("").sendKeys(instanceName);
    }

    public static void clickUpdateBtn() {
        updateBtn.shouldBe(enabled).click();
    }

    public static void clickCancelBtn() {
        cancelBtn.shouldBe(enabled).click();
    }

    public static void clickCloseBtn() {
        closeBtn.shouldBe(enabled).click();
    }

    public static void verifyErrorMessage(String errorMessage) {
        errorTxt.shouldHave(text(errorMessage));
    }

    public static void verifyClipboardValue() {
        urlTxt.shouldHave(attribute("href", Selenide.clipboard().getText() + "/"));
    }
}
