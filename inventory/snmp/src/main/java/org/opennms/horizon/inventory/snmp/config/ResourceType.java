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
import lombok.Data;
import lombok.Setter;

@XmlRootElement(name = "resourceType", namespace = "http://xmlns.opennms.org/xsd/config/datacollection")
@XmlAccessorType(XmlAccessType.NONE)
@Data
public class ResourceType {

    @Setter
    @XmlAttribute(name = "name", required = true)
    private String name;

    /**
     * Resource type label (this is what users see in the webUI)
     */
    @Setter
    @XmlAttribute(name = "label", required = true)
    private String label;

    /**
     * Resource label expression (this is what users see in the webUI for each
     * resource of this type)
     */
    @XmlAttribute(name = "resourceLabel")
    private String resourceLabel;

    /**
     * Selects a PersistenceSelectorStrategy that decides which data is
     * persisted and which is not.
     */
    @Setter
    @XmlElement(name = "persistenceSelectorStrategy")
    private PersistenceSelectorStrategy persistenceSelectorStrategy;

    /**
     * Selects a StorageStrategy that decides where data is stored.
     */
    @XmlElement(name = "storageStrategy")
    private StorageStrategy storageStrategy;
}
