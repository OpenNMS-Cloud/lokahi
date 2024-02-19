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
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Class Log.
 *
 * @version $Revision$ $Date$
 */
@XmlRootElement(name = "log")
@XmlAccessorType(XmlAccessType.FIELD)
// @ValidateUsing("event.xsd")
public class Log implements Serializable {
    private static final long serialVersionUID = 7684449895077223094L;

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field _header.
     */
    @XmlElement(name = "header", required = false)
    @Valid
    private Header _header;

    /**
     * Field _events.
     */
    @XmlElement(name = "events", required = true)
    @Size(min = 1)
    @Valid
    private Events _events;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public Log() {
        super();
    }

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Returns the value of field 'events'.
     *
     * @return the value of field 'Events'.
     */
    public Events getEvents() {
        return this._events;
    }

    /**
     * Returns the value of field 'header'.
     *
     * @return the value of field 'Header'.
     */
    public Header getHeader() {
        return this._header;
    }

    /**
     * Sets the value of field 'events'.
     *
     * @param events the value of field 'events'.
     */
    public void setEvents(final Events events) {
        this._events = events;
    }

    public void addEvent(final Event event) {
        assertEventsExists();
        this._events.addEvent(event);
    }

    public void addAllEvents(final Log log) {
        assertEventsExists();
        this._events.addAllEvents(log.getEvents());
    }

    protected void assertEventsExists() {
        if (this._events == null) {
            this._events = new Events();
        }
    }

    /**
     * Sets the value of field 'header'.
     *
     * @param header the value of field 'header'.
     */
    public void setHeader(final Header header) {
        this._header = header;
    }

    public void clear() {
        this._events = new Events();
    }

    @Override
    public String toString() {
        return new OnmsStringBuilder(this).toString();
    }
}
