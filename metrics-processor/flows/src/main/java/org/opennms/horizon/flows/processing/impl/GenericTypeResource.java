/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

import java.util.Objects;

public class GenericTypeResource extends DeferredGenericTypeResource {

    private final ResourceType m_resourceType;

    public GenericTypeResource(NodeLevelResource node, ResourceType resourceType, String instance) {
        super(node, Objects.requireNonNull(resourceType, "resourceType argument").getName(), instance);
        m_resourceType = Objects.requireNonNull(resourceType, "resourceType argument");
    }

    protected static String sanitizeInstance(String instance) {
        return instance
                .replaceAll("[\\s]+", "_")
                .replaceAll(":", "_")
                .replaceAll("\\\\", "_")
                .replaceAll("[\\[\\]]", "_")
                .replaceAll("/", "_");
    }

    public static String sanitizeInstanceStrict(String instance) {
        return instance.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    public ResourceType getResourceType() {
        return m_resourceType;
    }

    @Override
    public String getLabel(CollectionResource resource) {
        return "";
       // return getStorageStrategy().getResourceNameFromIndex(resource);
    }

    @Override
    public ResourcePath getPath(CollectionResource resource) {
        return ResourcePath.get("");
        //return getStorageStrategy().getRelativePathForAttribute(ResourcePath.get(), getStorageStrategy().getResourceNameFromIndex(resource));
    }

    @Override
    public Resource resolve() {
        return this;
    }

    @Override
    public String getTypeName() {
        return m_resourceType.getName();
    }

    @Override
    public String toString() {
        return String.format("GenericTypeResource[node=%s, instance=%s, unmodified-instance=%s, "
                + "resourceType=%s]",
                getParent(), getInstance(), getUnmodifiedInstance(), m_resourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParent(), getInstance(), getUnmodifiedInstance(), m_resourceType, getTimestamp());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof GenericTypeResource)) {
            return false;
        }
        GenericTypeResource other = (GenericTypeResource) obj;
        return super.equals(other)
                && Objects.equals(this.m_resourceType, other.m_resourceType);
    }


}
