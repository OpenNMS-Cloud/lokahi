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

package org.opennms.horizon.systemtests.steps;

import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.LoginPage;

public class LoginSteps {

    @Then("Login page appears")
    public void checkPopupIsVisible() {
        LoginPage.checkPageTitle();
    }

    @Then("set email address as {string}")
    public void setEmail(String email) {
        LoginPage.setUsername(email);
    }

    @Then("set password")
    public void setPassword() {
        LoginPage.setPassword("admin"); // TODO
    }

    @Then("click on 'Sign in' button")
    public void clickSignIn() {
        LoginPage.clickSignInBtn();
    }

    @Then("login to instance as {string} user")
    public void loginAsUser(String user) {
        setEmail(user);
        setPassword();
        clickSignIn();
    }
}
