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

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class CompAttrib implements Comparable<CompAttrib> {
    private static final Comparator<CompAttrib> COMPARATOR = Comparator.comparing(CompAttrib::getName);
    private String name;
    private String alias;
    private String type;
    private java.util.List<CompMember> _compMemberList = new java.util.ArrayList<>();

    public void addCompMember(final CompMember compMember)
        throws IndexOutOfBoundsException {
        this._compMemberList.add(compMember);
    }

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj )
            return true;

        if (obj instanceof CompAttrib) {
            CompAttrib temp = (CompAttrib)obj;
            boolean equals = Objects.equals(this.name, temp.name)
                && Objects.equals(this.alias, temp.alias)
                && Objects.equals(this.type, temp.type)
                && Objects.equals(this._compMemberList, temp._compMemberList);
            return equals;
        }
        return false;
    }

    public String getAlias() {
        return this.alias;
    }

    public java.util.List<CompMember> getCompMemberList() {
        return Collections.unmodifiableList(this._compMemberList);
    }

    public int getCompMemberCount() {
        return this._compMemberList.size();
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, alias, type, _compMemberList);
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public void setCompMemberList(final java.util.List<CompMember> vCompMemberList) {
        this._compMemberList.clear();
        this._compMemberList.addAll(vCompMemberList);
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void clearCompMembers() {
        this._compMemberList.clear();
    }

    @Override
    public int compareTo(final CompAttrib o) {
        return Objects.compare(this, o, COMPARATOR);
    }

}
