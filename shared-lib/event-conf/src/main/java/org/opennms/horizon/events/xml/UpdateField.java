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
package org.opennms.horizon.events.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Object used to identify which alert fields should be updated during Alert reduction.
 *
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 */
@XmlRootElement(name = "update-field")
@XmlAccessorType(XmlAccessType.FIELD)
// @ValidateUsing("event.xsd")
public class UpdateField implements Serializable {

    private static final long serialVersionUID = 4780818827895098397L;

    @XmlAttribute(name = "field-name", required = true)
    private String m_fieldName;

    @XmlAttribute(name = "update-on-reduction", required = false)
    private Boolean m_updateOnReduction = Boolean.TRUE;

    @XmlAttribute(name = "value-expression", required = false)
    private String m_valueExpression;

    public String getFieldName() {
        return m_fieldName;
    }

    public void setFieldName(String fieldName) {
        m_fieldName = fieldName;
    }

    public Boolean isUpdateOnReduction() {
        return m_updateOnReduction;
    }

    public void setUpdateOnReduction(Boolean update) {
        m_updateOnReduction = update;
    }

    public String getValueExpression() {
        return m_valueExpression;
    }

    public void setValueExpression(String valueExpression) {
        m_valueExpression = valueExpression;
    }
}
