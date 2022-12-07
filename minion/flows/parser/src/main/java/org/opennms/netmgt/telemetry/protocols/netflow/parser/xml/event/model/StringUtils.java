/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.telemetry.protocols.netflow.parser.xml.event.model;

public class StringUtils {

    public static boolean equalsTrimmed(String a, String b) {
        if (a == null) {
            return false;
        }

        int alen = a.length();
        final int blen = b.length();

        // Fail fast: If B is longer than A, B cannot be a substring of A
        if (blen > alen) {
            return false;
        }

        // Find the index of the first non-whitespace character in A
        int i = 0;
        while ((i < alen) && (a.charAt(i) <= ' ')) {
            i++;
        }

        // Match the subsequent characters in A to those in B
        int j = 0;
        while ((i < alen && j < blen)) {
            if (a.charAt(i) != b.charAt(j)) {
                return false;
            }
            i++;
            j++;
        }

        // If we've reached the end of A, then we have a match
        if (i == alen) {
            return true;
        }

        // "Trim" the whitespace characters off the end of A
        while ((i < alen) && (a.charAt(alen - 1) <= ' ')) {
            alen--;
        }

        // If only whitespace characters remained on A, then we have a match
        if (alen - i == 0) {
            return true;
        }

        // There are extra characters at the tail of A, that don't show up in B
        return false;
    }
}
