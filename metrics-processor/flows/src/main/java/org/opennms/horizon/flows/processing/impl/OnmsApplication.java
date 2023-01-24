/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.base.MoreObjects;


/**
 * An Application is a grouping of services that belong together.
 * They can run in different locations.
 * An example would be "website", or "database".
 */
public class OnmsApplication implements Comparable<OnmsApplication> {

    private Integer id;

    private String name;

    private Set<OnmsMonitoredService> monitoredServices = new LinkedHashSet<>();

    /**
     * These are locations from where the application is monitored.
     */
    private Set<OnmsMonitoringLocation> perspectiveLocations = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<OnmsMonitoredService> getMonitoredServices() {
        return monitoredServices;
    }

    public void setMonitoredServices(Set<OnmsMonitoredService> services) {
        monitoredServices = services;
    }

    public void addMonitoredService(OnmsMonitoredService service) {
        getMonitoredServices().add(service);
    }

    public void removeMonitoredService(OnmsMonitoredService service) {
        getMonitoredServices().remove(service);
    }

    public Set<OnmsMonitoringLocation> getPerspectiveLocations() {
        return this.perspectiveLocations;
    }

    public void setPerspectiveLocations(Set<OnmsMonitoringLocation> perspectiveLocations) {
        this.perspectiveLocations = perspectiveLocations;
    }

    public void addPerspectiveLocation(OnmsMonitoringLocation perspectiveLocation) {
        getPerspectiveLocations().add(perspectiveLocation);
    }

    @Override
    public int compareTo(OnmsApplication o) {
        return getName().compareTo(o.getName());
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .add("name", getName())
        .toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OnmsApplication) {
            OnmsApplication app = (OnmsApplication)obj;
            return getName().equals(app.getName());
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
