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

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class EditInstancePage {

    public static final ElementsCollection usersRow = $$("tbody tr");
    public static final SelenideElement usersTable = $("tbody");
    public static final SelenideElement deleteInstanceBtn = $("#delete-btn-cloud-instance");
    public static final SelenideElement instanceUrlTxt = $("#cloud-details-instance-url");

    public static void verifyNumberOfUsers(int count) {
        usersRow.shouldHave(CollectionCondition.size(count));
    }

    public static void verifyUserEmailInTable(String email) {
        usersTable.find(byText(email)).shouldBe(visible);
    }

    public static String getInstanceUrl() {
        return instanceUrlTxt.shouldBe(visible).getAttribute("href");
    }

    public static void clickDeleteInstance() {
        deleteInstanceBtn.shouldBe(enabled).click();
    }

    public static void clickOnInstanceUrl() {
        instanceUrlTxt.shouldBe(enabled).click();
    }
}
