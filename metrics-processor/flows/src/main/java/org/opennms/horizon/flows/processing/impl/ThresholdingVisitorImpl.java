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

package org.opennms.horizon.flows.processing.impl;

import java.util.Date;

import org.opennms.horizon.flows.processing.thresholding.ThresholdingVisitor;

public class ThresholdingVisitorImpl implements ThresholdingVisitor {
    @Override
    public void visitCollectionSet(CollectionSet set) {

    }

    @Override
    public void visitResource(CollectionResource resource) {

    }

    @Override
    public void visitGroup(AttributeGroup group) {

    }

    @Override
    public void visitAttribute(CollectionAttribute attribute) {

    }

    @Override
    public void completeAttribute(CollectionAttribute attribute) {

    }

    @Override
    public void completeGroup(AttributeGroup group) {

    }

    @Override
    public void completeResource(CollectionResource resource) {

    }

    @Override
    public void completeCollectionSet(CollectionSet set) {

    }

    @Override
    public Date getCollectionTimestamp() {
        return null;
    }

    @Override
    public boolean hasThresholds() {
        return false;
    }

    @Override
    public void reload() {

    }

    @Override
    public void setCounterReset(boolean counterReset) {

    }
}
