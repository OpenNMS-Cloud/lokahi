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
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class EditInstancePage {

    private static final SelenideElement titleTxt = $("div.header-container");
    private static final SelenideElement header = $("h2");
    private static final ElementsCollection usersRow = $$("tbody tr");
    private static final SelenideElement usersTable = $("tbody");
    private static final SelenideElement searchUserInp = $("#cloud-details-users-search");
    private static final SelenideElement deleteInstanceBtn = $("#delete-btn-cloud-instance");
    private static final SelenideElement editNameBtn = $("#edit-btn-cloud-instance");
    private static final SelenideElement instanceUrlTxt = $("#cloud-details-instance-url");
    private static final SelenideElement copyUrlBtn = $("#copy-url-btn");
    private static final SelenideElement goBackBtn = $("#cloud-details-back");

    public static void clickGoBackButton() {
        goBackBtn.shouldBe(enabled).click();
    }

    public static void verifyPageTitle() {
        titleTxt.shouldHave(text("Cloud Instance Details"));
    }

    public static void verifyNumberOfUsers(int count) {
        usersRow.shouldHave(CollectionCondition.size(count));
    }

    public static void verifyUserEmailInTable(String email) {
        usersTable.find(byText(email)).shouldBe(visible);
    }

    public static String getInstanceUrl() {
        return instanceUrlTxt.shouldBe(visible).getAttribute("href");
    }

    public static void clickCopyURLButton() {
        copyUrlBtn.shouldBe(enabled).click();
    }

    public static void clickDeleteInstance() {
        deleteInstanceBtn.shouldBe(enabled).click();
    }

    public static void clickEditNameBtn() {
        editNameBtn.shouldBe(enabled).click();
    }

    public static void clickOnInstanceUrl() {
        instanceUrlTxt.shouldBe(enabled).click();
    }

    public static void verifyInstanceName(String instanceName) {
        header.shouldHave(text("Instance Name: " + instanceName));
    }

    public static void verifySearchFieldIsEmpty() {
        searchUserInp.shouldHave(exactText(""), exactValue(""));
    }
}
