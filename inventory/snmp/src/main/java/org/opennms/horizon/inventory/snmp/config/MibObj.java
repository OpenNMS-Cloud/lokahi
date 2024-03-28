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
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@XmlRootElement(name = "mibObj", namespace = "http://xmlns.opennms.org/xsd/config/datacollection")
@XmlAccessorType(XmlAccessType.NONE)
@Data
public class MibObj {

    /**
     * object identifier
     */
    @XmlAttribute(name = "oid", required = true)
    private String oid;

    /**
     * instance identifier. Only valid instance identifier values are a
     * positive integer value or the keyword "ifIndex" which indicates that
     * the ifIndex of the interface is to be substituted for the instance
     * value for each interface the oid is retrieved for.
     */
    @XmlAttribute(name = "instance", required = true)
    private String instance;

    /**
     * a human readable name for the object (such as "ifOctetsIn"). NOTE: This
     * value is used as the RRD file name and data source name. RRD only
     * supports data source names up to 19 chars in length. If the SNMP data
     * collector encounters an alias which exceeds 19 characters it will be
     * truncated.
     */
    @XmlAttribute(name = "alias", required = true)
    private String alias;

    /**
     * SNMP data type SNMP supported types: counter, gauge, timeticks,
     * integer, octetstring, string. The SNMP type is mapped to one of two RRD
     * supported data types COUNTER or GAUGE, or the string.properties file.
     * The mapping is as follows: SNMP counter -&gt; RRD COUNTER; SNMP gauge,
     * timeticks, integer, octetstring -&gt; RRD GAUGE; SNMP string -&gt; String
     * properties file
     */
    @XmlAttribute(name = "type", required = true)
    private String type;

    /**
     * Maximum Value. In order to correctly manage counter wraps, it is
     * possible to add a maximum value for a collection. For example, a 32-bit
     * counter would have a max value of 4294967295.
     */
    @XmlAttribute(name = "maxval", required = false)
    private String maxval;

    /**
     * Minimum Value. For completeness, adding the ability to use a minimum
     * value.
     */
    @XmlAttribute(name = "minval", required = false)
    private String minval;
}
