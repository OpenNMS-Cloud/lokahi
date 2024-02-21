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
package org.opennms.horizon.events.conf.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.util.Objects;
import org.opennms.horizon.events.util.ConfigUtils;
import org.opennms.horizon.events.util.ValidateUsing;

/**
 * The trouble ticket info with state on/off determining if
 *  action is taken on the trouble ticket
 */
@XmlRootElement(name = "tticket")
@XmlAccessorType(XmlAccessType.NONE)
@ValidateUsing("eventconf.xsd")
public class Tticket implements Serializable {
    private static final long serialVersionUID = 2L;

    @XmlValue
    private String m_content;

    @XmlAttribute(name = "state", required = false)
    private StateType m_state;

    public String getContent() {
        return m_content;
    }

    public void setContent(final String content) {
        m_content = ConfigUtils.normalizeString(content);
    }

    public StateType getState() {
        return m_state == null ? StateType.ON : m_state; // Default state is "on" according to the XSD
    }

    public void setState(final StateType state) {
        m_state = state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_content, m_state);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Tticket) {
            final Tticket that = (Tticket) obj;
            return Objects.equals(this.m_content, that.m_content) && Objects.equals(this.m_state, that.m_state);
        }
        return false;
    }
}
