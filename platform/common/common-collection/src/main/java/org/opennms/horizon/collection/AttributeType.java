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

package org.opennms.horizon.collection;

public enum AttributeType {
    GAUGE("gauge", true, "gauge32", "integer32"),
    COUNTER("counter", true, "counter32"),
    STRING("string", false);

    private final String name;
    private final boolean isNumeric;
    private final String[] aliases;

    private AttributeType(String name, boolean isNumeric, String... aliases) {
        this.name = name;
        this.isNumeric = isNumeric;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public boolean isNumeric() {
        return isNumeric;
    }

    public String[] getAliases() {
        return aliases;
    }

    /**
     * Parses the attribute type from the given string.
     *
     * @param typeAsString type
     * @return the matching attribute, or null if none was found
     */
    public static AttributeType parse(String typeAsString) {
        for (AttributeType type : AttributeType.values()) {
            if (type.getName().equalsIgnoreCase(typeAsString)) {
                return type;
            } else {
                // Attribute types can be referred to by many names
                for (String alias : type.getAliases()) {
                    if (alias.equalsIgnoreCase(typeAsString)) {
                        return type;
                    }
                }
            }
        }
        return null;
    }
}
