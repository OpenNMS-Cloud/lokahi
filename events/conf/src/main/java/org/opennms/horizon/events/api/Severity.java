/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.events.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenNMS severity enumeration.
 *
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 */
public enum Severity implements Serializable {
    // Keep this ordered by ID so we can use the internal enum compareTo
    INDETERMINATE(1, "Indeterminate", "lightblue"),
    CLEARED(2, "Cleared", "white"),
    NORMAL(3, "Normal", "green"),
    WARNING(4, "Warning", "cyan"),
    MINOR(5, "Minor", "yellow"),
    MAJOR(6, "Major", "orange"),
    CRITICAL(7, "Critical", "red");

    private static final Map<Integer, Severity> m_idMap;

    private int m_id;
    private String m_label;
    private String m_color;

    static {
        m_idMap = new HashMap<Integer, Severity>(values().length);
        for (final Severity severity : values()) {
            m_idMap.put(severity.getId(), severity);
        }
    }

    private Severity(final int id, final String label, final String color) {
        m_id = id;
        m_label = label;
        m_color = color;
    }
    
    /**
     * <p>getId</p>
     *
     * @return a int.
     */
    public int getId() {
        return m_id;
    }
    
    /**
     * <p>getLabel</p>
     *
     * @return a {@link String} object.
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * <p>getColor</p>
     *
     * @return a {@link String} object.
     */
    public String getColor() {
        return m_color;
    }

    /**
     * <p>isLessThan</p>
     *
     * @param other a {@link Severity} object.
     * @return a boolean.
     */
    public boolean isLessThan(final Severity other) {
        return compareTo(other) < 0;
    }

    /**
     * <p>isLessThanOrEqual</p>
     *
     * @param other a {@link Severity} object.
     * @return a boolean.
     */
    public boolean isLessThanOrEqual(final Severity other) {
        return compareTo(other) <= 0;
    }

    /**
     * <p>isGreaterThan</p>
     *
     * @param other a {@link Severity} object.
     * @return a boolean.
     */
    public boolean isGreaterThan(final Severity other) {
        return compareTo(other) > 0;
    }
    
    /**
     * <p>isGreaterThanOrEqual</p>
     *
     * @param other a {@link Severity} object.
     * @return a boolean.
     */
    public boolean isGreaterThanOrEqual(final Severity other) {
        return compareTo(other) >= 0;
    }
    
    /**
     * <p>get</p>
     *
     * @param id a int.
     * @return a {@link Severity} object.
     */
    public static Severity get(final int id) {
        if (m_idMap.containsKey(id)) {
            return m_idMap.get(id);
        } else {
            throw new IllegalArgumentException("Cannot create Severity from unknown ID " + id);
        }
    }

    /**
     * <p>get</p>
     *
     * @param label a {@link String} object.
     * @return a {@link Severity} object.
     */
    public static Severity get(final String label) {
        for (final Integer key : m_idMap.keySet()) {
            if (m_idMap.get(key).getLabel().equalsIgnoreCase(label)) {
                return m_idMap.get(key);
            }
        }
        return Severity.INDETERMINATE;
    }

    /**
     * <p>escalate</p>
     *
     * @param sev a {@link Severity} object.
     * @return a {@link Severity} object.
     */
    public static Severity escalate(final Severity sev) {
        if (sev.isLessThan(Severity.CRITICAL)) {
            return Severity.get(sev.getId()+1);
        } else {
            return Severity.get(sev.getId());
        }
    }

    public static List<String> names() {
        final List<String> names = new ArrayList<>();
        for (final Severity value : values()) {
            names.add(value.toString());
        }
        return names;
    }
}
