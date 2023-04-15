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

import static com.codeborne.selenide.Selenide.$;

public class DeleteInstancePopup {

    private static final SelenideElement popup = $("[data-ref-id='feather-dialog']");
    private static final SelenideElement confirmInstanceNameInp = $("#delete-instance-confirm-input");
    private static final SelenideElement deleteBtn = $("div.dialog-footer button.btn-primary");
    private static final SelenideElement cancelBtn = $("div.dialog-footer button.btn-secondary");

    public static void waitPopupIsDisplayed(boolean isVisible) {
        if (isVisible) {
            popup.shouldBe(Condition.visible);
        } else {
            popup.shouldBe(Condition.disappear);
        }
    }

    public static void setInstanceEmail(String instanceName) {
        confirmInstanceNameInp.shouldBe(Condition.enabled).setValue(instanceName);
    }

    public static void clickDelete() {
        deleteBtn.shouldBe(Condition.enabled).click();
    }

    public static void clickCancel() {
        cancelBtn.shouldBe(Condition.enabled).click();
    }
}
