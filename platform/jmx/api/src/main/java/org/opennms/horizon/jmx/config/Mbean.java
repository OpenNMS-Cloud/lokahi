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
import java.util.Objects;

public class Mbean {
    private String name;
    private String objectname;
    private String keyfield;
    private String exclude;
    private String keyAlias;
    private String resourceType;
    private java.util.List<Attrib> attribList = new java.util.ArrayList<>();
    private java.util.List<String> includeMbeanList = new java.util.ArrayList<>();
    private java.util.List<CompAttrib> compAttribList = new java.util.ArrayList<>();

    public void addAttrib(final Attrib vAttrib) {
        this.attribList.add(vAttrib);
    }

    public void addCompAttrib(final CompAttrib vCompAttrib) {
        this.compAttribList.add(vCompAttrib);
    }

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj )
            return true;

        if (obj instanceof Mbean) {
            Mbean temp = (Mbean)obj;

            boolean equals = Objects.equals(name, temp.name)
                && Objects.equals(objectname, temp.objectname)
                && Objects.equals(keyfield, temp.keyfield)
                && Objects.equals(exclude, temp.exclude)
                && Objects.equals(keyAlias, temp.keyAlias)
                && Objects.equals(attribList, temp.attribList)
                && Objects.equals(includeMbeanList, temp.includeMbeanList)
                && Objects.equals(compAttribList, temp.compAttribList)
                && Objects.equals(resourceType, temp.resourceType);
            return equals;
        }
        return false;
    }

    public java.util.List<Attrib> getAttribList() {
        return Collections.unmodifiableList(this.attribList);
    }

    /**
     * @return the size of this collection
     */
    public int getAttribCount() {
        return this.attribList.size();
    }

    public java.util.List<CompAttrib> getCompAttribList() {
        return Collections.unmodifiableList(this.compAttribList);
    }

    /**
     * @return the size of this collection
     */
    public int getCompAttribCount() {
        return this.compAttribList.size();
    }

    public String getExclude( ) {
        return this.exclude;
    }


    public java.util.List<String> getIncludeMbeanCollection() {
        return this.includeMbeanList;
    }

    public int getIncludeMbeanCount() {
        return this.includeMbeanList.size();
    }

    public String getKeyAlias() {
        return this.keyAlias;
    }

    public String getKeyfield() {
        return this.keyfield;
    }

    public String getName() {
        return this.name;
    }

    public String getObjectname() {
        return this.objectname;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, objectname, keyfield, exclude, keyAlias, attribList, includeMbeanList, compAttribList, resourceType);
    }

    public void setAttribCollection(final java.util.List<Attrib> attribList) {
        this.attribList = attribList;
    }


    public void setCompAttribCollection(final java.util.List<CompAttrib> compAttribList) {
        this.compAttribList = compAttribList;
    }

    public void setExclude( final String exclude) {
        this.exclude = exclude;
    }

    public void setIncludeMbeanCollection(final java.util.List<String> includeMbeanList) {
        this.includeMbeanList = includeMbeanList;
    }

    public void setKeyAlias(final String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public void setKeyfield(final String keyfield) {
        this.keyfield = keyfield;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setObjectname(final String objectname) {
        this.objectname = objectname;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void clearAttribs() {
        this.attribList.clear();
    }

    public void clearCompAttribs() {
        this.compAttribList.clear();
    }
}
