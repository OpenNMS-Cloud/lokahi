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
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.List;
import lombok.Data;

@XmlRootElement(name = "property", namespace = "http://xmlns.opennms.org/xsd/config/datacollection")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class MibObjProperty {

    /** The instance.
     */
    @XmlAttribute(name = "instance", required = true)
    private String instance;

    /** The alias.
     */
    @XmlAttribute(name = "alias", required = false)
    private String alias;

    /** The class name.
     */
    @XmlAttribute(name = "class-name", required = false)
    private String className;

    /** The parameters.
     */
    @XmlElement(name = "parameter", required = false)
    private List<Parameter> parameters;

    /** The group name.
     */
    @XmlTransient
    private String groupName;

    /**
     * Gets the value of an existing parameter.
     *
     * @param key the key
     * @return the parameter
     */
    public String getParameterValue(String key) {
        return getParameterValue(key, null);
    }

    /**
     * Gets the value of an existing parameter.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the parameter
     */
    public String getParameterValue(String key, String defaultValue) {
        for (Parameter p : parameters) {
            if (p.getKey().equals(key)) {
                return p.getValue();
            }
        }
        return defaultValue;
    }

    /**
     * Adds a new parameter.
     *
     * @param key the key
     * @param value the value
     */
    public void addParameter(String key, String value) {
        parameters.add(new Parameter(key, value));
    }
}
