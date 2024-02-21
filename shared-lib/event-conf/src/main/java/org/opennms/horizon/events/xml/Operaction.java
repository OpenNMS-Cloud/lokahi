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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;

/**
 * The operator action to be taken when this event occurs
 *  with state controlling if action takes place. The menutext gets
 *  displayed in the UI.
 *
 * @version $Revision$ $Date$
 */
@XmlRootElement(name = "operaction")
@XmlAccessorType(XmlAccessType.FIELD)
public class Operaction implements Serializable {
    private static final long serialVersionUID = -4021848582976244135L;

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * internal content storage
     */
    @XmlValue
    @NotNull
    private String _content = "";

    /**
     * Field _state.
     */
    @XmlAttribute(name = "state")
    @Pattern(regexp = "(on|off)")
    private String _state = "on";

    /**
     * Field _menutext.
     */
    @XmlAttribute(name = "menutext", required = true)
    @NotNull
    private String _menutext;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public Operaction() {
        super();
        setContent("");
        setState("on");
    }

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Returns the value of field 'content'. The field 'content'
     * has the following description: internal content storage
     *
     * @return the value of field 'Content'.
     */
    public String getContent() {
        return this._content;
    }

    /**
     * Returns the value of field 'menutext'.
     *
     * @return the value of field 'Menutext'.
     */
    public String getMenutext() {
        return this._menutext;
    }

    /**
     * Returns the value of field 'state'.
     *
     * @return the value of field 'State'.
     */
    public String getState() {
        return this._state;
    }

    /**
     * Sets the value of field 'content'. The field 'content' has
     * the following description: internal content storage
     *
     * @param content the value of field 'content'.
     */
    public void setContent(final String content) {
        this._content = content;
    }

    /**
     * Sets the value of field 'menutext'.
     *
     * @param menutext the value of field 'menutext'.
     */
    public void setMenutext(final String menutext) {
        this._menutext = menutext;
    }

    /**
     * Sets the value of field 'state'.
     *
     * @param state the value of field 'state'.
     */
    public void setState(final String state) {
        this._state = state;
    }

    @Override
    public String toString() {
        return new OnmsStringBuilder(this).toString();
    }
}
