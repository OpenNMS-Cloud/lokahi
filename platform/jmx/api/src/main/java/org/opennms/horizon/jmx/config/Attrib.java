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

package org.opennms.horizon.jmx.config;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.opennms.horizon.collection.AttributeType;

public class Attrib implements Serializable, Comparable<Attrib> {

    private static final Comparator<Attrib> COMPARATOR = Comparator.comparing(Attrib::getName);
    private String name;
    private String alias;
    private AttributeType type;
    private String maxval;
    private String minval;

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj )
            return true;

        if (obj instanceof Attrib) {
            Attrib temp = (Attrib)obj;
            boolean equals = Objects.equals(this.name, temp.name)
                && Objects.equals(this.alias, temp.alias)
                && Objects.equals(this.type, temp.type)
                && Objects.equals(this.maxval, temp.maxval)
                && Objects.equals(this.minval, temp.minval);
            return equals;
        }
        return false;
    }

    public String getAlias() {
        return this.alias;
    }

    public String getMaxval() {
        return this.maxval;
    }

    public String getMinval() {
        return this.minval;
    }

    public String getName() {
        return this.name;
    }

    public AttributeType getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias, type, maxval, minval);
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public void setMaxval(final String maxval) {
        this.maxval = maxval;
    }

    public void setMinval(final String minval) {
        this.minval = minval;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setType(final AttributeType type) {
        this.type = type;
    }

    @Override
    public int compareTo(final Attrib o) {
        return Objects.compare(this, o, COMPARATOR);
    }
}
