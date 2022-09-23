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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Rrd {
    private int step = 0;
    private List<String> rraList = new ArrayList<>();

    public void addRra(final String rra) {
        rraList.add(rra);
    }

    public List<String> getRraCollection() {
        return rraList;
    }

    public int getRraCount() {
        return rraList.size();
    }

    public int getStep() {
        return step;
    }

    public boolean hasStep() {
        return step > 0;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void setRraList(List<String> rraList) {
        this.rraList = rraList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rrd rrd = (Rrd) o;
        return step == rrd.step && Objects.equals(rraList, rrd.rraList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(step, rraList);
    }
}
