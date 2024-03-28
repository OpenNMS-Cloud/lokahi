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

@XmlRootElement(name = "systemDef", namespace = "http://xmlns.opennms.org/xsd/config/datacollection")
@XmlAccessorType(XmlAccessType.NONE)
@Data
public class SystemDef {
    /*
     * Note that we do not do JAXB field definitions like we usually do, since
     * we have to do some trickery to remain compatible with the XSD which expects
     * a <choice> between sysoid and sysoidmask, and there's basically no way to
     * implement that with JAXB when the types are the same.
     */

    private String name;

    private SystemDefChoice systemDefChoice = new SystemDefChoice();

    /**
     * List of MIB groups to be collected for the system
     */
    private Collect collect;

    @XmlAttribute(name = "name", required = true)
    public String getName() {
        return name;
    }

    /* Make compatible with JAXB by proxying SystemDefChoice */
    @XmlElement(name = "sysoid")
    public String getSysoid() {
        return systemDefChoice == null ? null : systemDefChoice.getSysoid();
    }

    public void setSysoid(final String sysoid) {
        if (systemDefChoice == null) systemDefChoice = new SystemDefChoice();
        systemDefChoice.setSysoid(sysoid);
        systemDefChoice.setSysoidMask(null);
    }

    @XmlElement(name = "sysoidMask")
    public String getSysoidMask() {
        return systemDefChoice == null ? null : systemDefChoice.getSysoidMask();
    }

    public void setSysoidMask(final String sysoidMask) {
        if (systemDefChoice == null) systemDefChoice = new SystemDefChoice();
        systemDefChoice.setSysoid(null);
        systemDefChoice.setSysoidMask(sysoidMask);
    }

    /**
     * container for list of MIB groups to be collected for the system
     */
    @XmlElement(name = "collect")
    public Collect getCollect() {
        return collect;
    }
}
