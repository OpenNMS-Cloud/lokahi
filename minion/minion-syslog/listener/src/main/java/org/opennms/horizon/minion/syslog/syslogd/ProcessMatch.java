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
package org.opennms.horizon.minion.syslog.syslogd;




import org.opennms.horizon.minion.syslog.utils.ConfigUtils;
import org.opennms.horizon.minion.syslog.xml.ValidateUsing;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * String against which to match the process name; interpreted
 *  as a regular expression. If no process name is present in
 *  the incoming message, any process-match elements will be
 *  considered non-matches.
 */
@XmlRootElement(name = "process-match")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("syslog.xsd")
public class ProcessMatch implements Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * The regular expression
     */
    @XmlAttribute(name = "expression", required = true)
    private String m_expression;

    public ProcessMatch() {
    }

    public String getExpression() {
        return m_expression;
    }

    public void setExpression(final String expression) {
        m_expression = ConfigUtils.assertNotEmpty(expression, "expression");
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_expression);
    }

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }

        if (obj instanceof ProcessMatch) {
            final ProcessMatch that = (ProcessMatch)obj;
            return Objects.equals(this.m_expression, that.m_expression);
        }
        return false;
    }

}
