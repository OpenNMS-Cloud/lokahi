/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 
 * The @XmlJavaTypeAdapter annotation doesn't work properly when using
 * the @XmlElements annotation unless we explicitly declare the types
 * using @XmlElement. These are defined bellow as variables named workaround_*.
 * For further details, see:
 *     https://bugs.eclipse.org/bugs/show_bug.cgi?id=419310
 *     http://stackoverflow.com/questions/19266097/jaxb-moxy-using-xmlelements-with-xmladapter
 * 
 * @author jwhite
 */
public class CollectionResourceDTO {


    private final NodeLevelResource workaround_nlr = null;


    private final DeferredGenericTypeResource workaround_dgtr = null;


    private final GenericTypeResource workaround_gtr = null;


    private final NumericAttribute workaround_na = null;
    

    private final StringAttribute workaround_sa = null;


    private Resource resource;

    private List<Attribute<?>> attributes = new ArrayList<>();

    public CollectionResourceDTO() { }

    public CollectionResourceDTO(Resource resource, List<Attribute<?>> attributes) {
        this.resource = resource;
        this.attributes = attributes;
    }

    public Resource getResource() {
        // Resolve the resource and store the result.
        // For DeferredGenericTypeResource resources, this is used
        // to lookup the resource definition and construct
        // the appropriate GenericTypeResource
        this.resource = resource != null ? resource.resolve() : null;
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public List<Attribute<?>> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute<?>> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return String.format("CollectionSetAttributesDTO[resource=%s, attributes=%s]",
                resource, attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, attributes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof CollectionResourceDTO)) {
            return false;
        }
        CollectionResourceDTO other = (CollectionResourceDTO) obj;
        return Objects.equals(this.resource, other.resource)
               && Objects.equals(this.attributes, other.attributes);
    }

}
