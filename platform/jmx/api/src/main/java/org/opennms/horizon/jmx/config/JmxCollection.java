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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.opennms.horizon.collection.Rrd;


public class JmxCollection {
    private String name;
    /**
     * @deprecated
     */
    @Deprecated
    private int maxVarsPerPdu = 0;
    private Rrd rrd;
    private List<Mbean> mbeans = new ArrayList<>();
    private List<String> importMbeansList = new ArrayList<>();

    /**
     * Gets the import MBeans list.
     *
     * @return the import MBeans list
     */
    public List<String> getImportGroupsList() {
        return importMbeansList;
    }

    /**
     * Sets the import MBeans list.
     *
     *
     * @param importMbeansList the new import MBeans list
     */
    public void setImportGroupsList(List<String> importMbeansList) {
        importMbeansList = importMbeansList;
    }

    /**
     * Checks for import MBeans.
     *
     * @return true, if successful
     */
    public boolean hasImportMbeans() {
        return importMbeansList != null && !importMbeansList.isEmpty();
    }

    /**
     * Overrides the java.lang.Object.equals method.
     *
     * @param obj
     * @return true if the objects are equal.
     */
    @Override()
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (obj instanceof JmxCollection) {
            JmxCollection temp = (JmxCollection) obj;

            boolean equals = Objects.equals(name, temp.name)
                && Objects.equals(maxVarsPerPdu, temp.maxVarsPerPdu)
                && Objects.equals(rrd, temp.rrd)
                && Objects.equals(mbeans, temp.mbeans);
            return equals;
        }
        return false;
    }

    public int getMaxVarsPerPdu(
    ) {
        return maxVarsPerPdu;
    }

    public List<Mbean> getMbeans() {
        return mbeans;
    }

    public String getName() {
        return name;
    }

    public Rrd getRrd() {
        return rrd;
    }

    /**
     * @return true if at least one MaxVarsPerPdu has been added
     */
    public boolean hasMaxVarsPerPdu() {
        return maxVarsPerPdu != 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, maxVarsPerPdu, rrd, mbeans);
    }

    /**
     * Method getMbeanCount.
     *
     * @return the size of this collection
     */
    public int getMbeanCount() {
        return this.mbeans.size();
    }

    public void addMbean(Mbean mbean) {
        if (mbean != null) {
            mbeans.add(mbean);
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setMaxVarsPerPdu(final int maxVarsPerPdu) {
        this.maxVarsPerPdu = maxVarsPerPdu;
    }

    public void setMbeans(final List<Mbean> mbeans) {
        this.mbeans = mbeans;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setRrd(final Rrd rrd) {
        this.rrd = rrd;
    }

    public void addMbeans(List<Mbean> mbeanList) {
        mbeans.addAll(mbeanList);
    }
}
