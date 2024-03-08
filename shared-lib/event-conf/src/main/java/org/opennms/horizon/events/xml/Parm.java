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

// ---------------------------------/
// - Imported classes and packages -/
// ---------------------------------/

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.StringJoiner;

/**
 * A varbind from the trap
 *
 * @version $Revision$ $Date$
 */
@XmlRootElement(name = "parm")
@XmlAccessorType(XmlAccessType.FIELD)
// @ValidateUsing("event.xsd")
public class Parm implements Serializable {
    private static final long serialVersionUID = 2841420030575276257L;

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * parm name
     */
    @XmlElement(name = "parmName", required = true)
    @NotNull
    private String _parmName;

    /**
     * parm value
     */
    @XmlElement(name = "value", required = true)
    @NotNull
    @Valid
    private Value _value;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public Parm() {
        super();
    }

    // -----------/
    // - Methods -/
    // -----------/

    public Parm(final String name, final String value) {
        this();
        setParmName(name);
        this._value = new Value(value);
    }

    /**
     * Returns the value of field 'parmName'. The field 'parmName'
     * has the following description: parm name
     *
     * @return the value of field 'ParmName'.
     */
    public String getParmName() {
        return this._parmName;
    }

    /**
     * Returns the value of field 'value'. The field 'value' has
     * the following description: parm value
     *
     * @return the value of field 'Value'.
     */
    public Value getValue() {
        return this._value;
    }

    /**
     * Sets the value of field 'parmName'. The field 'parmName' has
     * the following description: parm name
     *
     * @param parmName the value of field 'parmName'.
     */
    public void setParmName(final String parmName) {
        this._parmName = parmName;
    }

    /**
     * Sets the value of field 'value'. The field 'value' has the
     * following description: parm value
     *
     * @param value the value of field 'value'.
     */
    public void setValue(final Value value) {
        this._value = value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Parm.class.getSimpleName() + "[", "]")
                .add("parmName='" + _parmName + "'")
                .add("value=" + _value)
                .toString();
    }

    public boolean isValid() {
        return getParmName() != null && getValue() != null;
    }
}
