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


package org.opennms.horizon.systemtests.steps.portal;

import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.portal.PortalSnackBar;

public class PortalSnackBarSteps {

    @Then("the IT Administrator gets success snackbar message {string}")
    public void checkSnackBarMessage(String message) {
        PortalSnackBar.verifySuccessSnackBarMessage(message);
    }

    @Then("the IT Administrator gets error snackbar message {string}")
    public void checkErrorSnackBarMessage(String message) {
        PortalSnackBar.verifyErrorSnackBarMessage(message);
    }

    @Then("the IT Administrator doesn't see a snackbar message")
    public void checkNoSnackBar() {
        PortalSnackBar.snackBarIsHidden();
    }
}
