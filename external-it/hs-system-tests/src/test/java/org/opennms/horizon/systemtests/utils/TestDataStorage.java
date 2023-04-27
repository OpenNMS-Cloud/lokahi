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

package org.opennms.horizon.systemtests.utils;

import org.opennms.horizon.systemtests.keyvalue.SecretsStorage;

import static org.opennms.horizon.systemtests.CucumberHooks.MINIONS;

public class TestDataStorage {
    public static String mapUserToEmail(String email) {
        return switch (email) {
            case "ADMIN" -> SecretsStorage.adminUserEmail;
            case "OKTA_USER" -> SecretsStorage.oktaUserEmail;
            default -> email;
        };
    }

    /**
     * Gets the name of the minion as it appears in Minion table as label
     * @param name Name of the minion from the feature file
     * @return name as label
     */
    public static String getMinionName(String name) {
        return switch (name) {
            case "DEFAULT" -> MINIONS.get(0).minionId;
            default -> MINIONS.get(0).minionId.toUpperCase();
        };
    }

    /**
     * String to boolean converter as Gherkins has no Boolean variable in steps definition
     * @param bool String representation of boolean
     * @return Boolean return value
     */
    public static boolean stringToBoolean(String bool) {
        return switch (bool) {
            case "TRUE" -> true;
            default -> false;
        };
    }
}
