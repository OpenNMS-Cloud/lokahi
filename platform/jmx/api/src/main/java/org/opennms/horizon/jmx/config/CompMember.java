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

import java.util.Comparator;
import java.util.Objects;

import org.opennms.horizon.collection.AttributeType;

public class CompMember implements Comparable<CompMember>{
    private static final Comparator<CompMember> COMPARATOR = Comparator.comparing(CompMember::getName);
    private String name;
    private String alias;
    private AttributeType type;
    private String maxval;
    private String minval;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public String getMaxval() {
        return maxval;
    }

    public void setMaxval(String maxval) {
        this.maxval = maxval;
    }

    public String getMinval() {
        return minval;
    }

    public void setMinval(String minval) {
        this.minval = minval;
    }

    @Override
    public int compareTo(final CompMember o) {
        return Objects.compare(this, o, COMPARATOR);
    }

    public Attrib toAttrib() {
        Attrib attrib = new Attrib();
        attrib.setAlias(alias);
        attrib.setMaxval(maxval);
        attrib.setMinval(minval);
        attrib.setType(type);
        attrib.setName(name);
        return attrib;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompMember that = (CompMember) o;
        return Objects.equals(name, that.name) && Objects.equals(alias, that.alias) && type == that.type && Objects.equals(maxval, that.maxval) && Objects.equals(minval, that.minval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias, type, maxval, minval);
    }
}
