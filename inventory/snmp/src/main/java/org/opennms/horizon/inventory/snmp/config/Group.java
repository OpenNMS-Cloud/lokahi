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
import lombok.Data;

@XmlRootElement(name = "group", namespace = "http://xmlns.opennms.org/xsd/config/datacollection")
@XmlAccessorType(XmlAccessType.NONE)
@Data
public class Group {

    @XmlAttribute(name = "name", required = true)
    private String name;

    /**
     * <p>
     * Interface type.
     * </p>
     * <p>Indicates the interface types from which the groups MIB objects are to
     * be collected.</p>
     * <p>
     * Supports individual ifType values or comma-separated list of ifType
     * values in addition to "all" and "ignore" key words.
     * </p>
     * <p>
     * For example:
     * </p>
     * <ul>
     * <li>"6" indicates that OIDs from this MIB group are to be collected
     * only for ethernet interfaces (ifType = 6)</li>
     * <li>"6,22" indicates that OIDs from this MIB group are to be collected
     * only for ethernet and serial interfaces</li>
     * <li>"all" indicates that the OIDs from this MIB group are to be
     * collected for all interfaces regardless of ifType</li>
     * <li>"ignore" indicates that OIDs from this MIB group are node-level
     * objects.</li>
     * </ul>
     * <p>
     * Sample ifType descriptions/values: (Refer to
     * http://www.iana.org/assignments/ianaiftype-mib for a comprehensive
     * list.)
     * </p>
     * <ul>
     * <li>ethernetCsmacd 6</li>
     * <li>iso8825TokenRing 9</li>
     * <li>fddi 15</li>
     * <li>sdlc 17</li>
     * <li>basicISDN 20</li>
     * <li>primaryISDN 21</li>
     * <li>propPointToPointSerial 22</li>
     * <li>ppp 23</li>
     * <li>atm 37</li>
     * <li>sonet 39</li>
     * <li>opticalChannel 195</li>
     * </ul>
     */
    @XmlAttribute(name = "ifType", required = true)
    private String ifType;

    /**
     * a MIB object
     */
    @XmlElement(name = "mibObj")
    private List<MibObj> mibObjects = new ArrayList<>();

    public void addMibObj(final MibObj mibObj) throws IndexOutOfBoundsException {
        this.mibObjects.add(mibObj);
    }

    public boolean removeMibObj(final MibObj mibObj) {
        return this.mibObjects.remove(mibObj);
    }
}
