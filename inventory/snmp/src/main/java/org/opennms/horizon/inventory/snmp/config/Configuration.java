/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.snmp.config;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Data;

@XmlRootElement(name = "datacollection-group", namespace = "http://xmlns.opennms.org/xsd/config/datacollection")
@XmlAccessorType(XmlAccessType.NONE)
@Data
public class Configuration {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "resourceType")
    private List<ResourceType> resourceTypes = new ArrayList<>();

    @XmlElement(name = "group")
    private List<Group> groups = new ArrayList<>();

    @XmlElement(name = "systemDef")
    private List<SystemDef> systemDefs = new ArrayList<>();

    public Configuration() {}

    public void addResourceType(final ResourceType resourceType) throws IndexOutOfBoundsException {
        resourceTypes.add(resourceType);
    }

    public boolean removeResourceType(final ResourceType resourceType) {
        return resourceTypes.remove(resourceType);
    }

    public void addGroup(final Group group) throws IndexOutOfBoundsException {
        groups.add(group);
    }

    public boolean removeGroup(final Group group) {
        return groups.remove(group);
    }

    public void addSystemDef(final SystemDef systemDef) throws IndexOutOfBoundsException {
        this.systemDefs.add(systemDef);
    }

    public boolean removeSystemDef(final SystemDef systemDef) {
        return this.systemDefs.remove(systemDef);
    }

    public Optional<Group> findGroupByName(final String name) {
        return this.groups.stream()
                .filter(group -> Objects.equals(group.getName(), name))
                .findAny();
    }

    public Optional<ResourceType> findResourceType(final String name) {
        return this.resourceTypes.stream()
                .filter(resourceType -> resourceType.getName().equals(name))
                .findAny();
    }
}
