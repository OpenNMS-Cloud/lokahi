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
import java.util.ArrayList;
import java.util.List;

/**
 * Class Events.
 *
 * @version $Revision$ $Date$
 */
@XmlRootElement(name = "events")
@XmlAccessorType(XmlAccessType.FIELD)
// @ValidateUsing("event.xsd")
public class Events implements Serializable {
    private static final long serialVersionUID = -6993861737101274987L;

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field _eventList.
     */
    @XmlElement(name = "event")
    @Size(min = 1)
    @Valid
    private List<Event> _eventList;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public Events() {
        super();
        this._eventList = new ArrayList<>();
    }

    // -----------/
    // - Methods -/
    // -----------/

    /**
     *
     *
     * @param vEvent
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addEvent(final Event vEvent) throws IndexOutOfBoundsException {
        if (this._eventList == null) {
            this._eventList = new ArrayList<>();
        }
        this._eventList.add(vEvent);
    }

    /**
     *
     *
     * @param index
     * @param vEvent
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addEvent(final int index, final Event vEvent) throws IndexOutOfBoundsException {
        this._eventList.add(index, vEvent);
    }

    /**
     * Method enumerateEvent.
     *
     * @return an Enumeration over all possible elements of this
     * collection
     */
    public java.util.Enumeration<Event> enumerateEvent() {
        return java.util.Collections.enumeration(this._eventList);
    }

    /**
     * Method getEvent.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the Event
     * at the given index
     */
    public Event getEvent(final int index) throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._eventList.size()) {
            throw new IndexOutOfBoundsException(
                    "getEvent: Index value '" + index + "' not in range [0.." + (this._eventList.size() - 1) + "]");
        }

        return (Event) _eventList.get(index);
    }

    /**
     * Method getEvent.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     *
     * @return this collection as an Array
     */
    public Event[] getEvent() {
        Event[] array = new Event[0];
        return (Event[]) this._eventList.toArray(array);
    }

    /**
     * Method getEventCollection.Returns a reference to
     * '_eventList'. No type checking is performed on any
     * modifications to the Vector.
     *
     * @return a reference to the Vector backing this class
     */
    public List<Event> getEventCollection() {
        return this._eventList;
    }

    /**
     * Method getEventCount.
     *
     * @return the size of this collection
     */
    public int getEventCount() {
        return this._eventList.size();
    }

    /**
     * Method iterateEvent.
     *
     * @return an Iterator over all possible elements in this
     * collection
     */
    public java.util.Iterator<Event> iterateEvent() {
        return this._eventList.iterator();
    }

    /**
     */
    public void removeAllEvent() {
        this._eventList.clear();
    }

    /**
     * Method removeEvent.
     *
     * @param vEvent
     * @return true if the object was removed from the collection.
     */
    public boolean removeEvent(final Event vEvent) {
        return _eventList.remove(vEvent);
    }

    /**
     * Method removeEventAt.
     *
     * @param index
     * @return the element removed from the collection
     */
    public Event removeEventAt(final int index) {
        return this._eventList.remove(index);
    }

    /**
     *
     *
     * @param index
     * @param vEvent
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setEvent(final int index, final Event vEvent) throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._eventList.size()) {
            throw new IndexOutOfBoundsException(
                    "setEvent: Index value '" + index + "' not in range [0.." + (this._eventList.size() - 1) + "]");
        }

        this._eventList.set(index, vEvent);
    }

    /**
     *
     *
     * @param vEventArray
     */
    public void setEvent(final Event[] vEventArray) {
        // -- copy array
        _eventList.clear();

        for (int i = 0; i < vEventArray.length; i++) {
            this._eventList.add(vEventArray[i]);
        }
    }

    /**
     * Sets the value of '_eventList' by copying the given Vector.
     * All elements will be checked for type safety.
     *
     * @param vEventList the Vector to copy.
     */
    public void setEvent(final List<Event> vEventList) {
        // copy vector
        this._eventList.clear();

        this._eventList.addAll(vEventList);
    }

    /**
     * Sets the value of '_eventList' by setting it to the given
     * Vector. No type checking is performed.
     * @deprecated
     *
     * @param eventList the Vector to set.
     */
    public void setEventCollection(final List<Event> eventList) {
        this._eventList = eventList;
    }

    @Override
    public String toString() {
        return new OnmsStringBuilder(this).toString();
    }

    public void addAllEvents(Events events) {
        if (events == null) {
            return;
        }
        final List<Event> eventCollection = events.getEventCollection();
        if (eventCollection != null) {
            for (final Event e : eventCollection) {
                this.addEvent(e);
            }
        }
    }
}
