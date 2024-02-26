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

import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import org.opennms.horizon.events.util.ValidateUsing;

/**
 * Class EventReceipt.
 *
 * @version $Revision$ $Date$
 */
@XmlRootElement(name = "event-receipt")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("event.xsd")
public class EventReceipt implements Serializable {
    private static final long serialVersionUID = -3104058231772479313L;

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field _uuidList.
     */
    @XmlElement(name = "uuid")
    @Size(min = 1)
    private java.util.List<String> _uuidList;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public EventReceipt() {
        super();
        this._uuidList = new java.util.ArrayList<>();
    }

    // -----------/
    // - Methods -/
    // -----------/

    /**
     *
     *
     * @param vUuid
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addUuid(final String vUuid) throws IndexOutOfBoundsException {
        this._uuidList.add(vUuid);
    }

    /**
     *
     *
     * @param index
     * @param vUuid
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addUuid(final int index, final String vUuid) throws IndexOutOfBoundsException {
        this._uuidList.add(index, vUuid);
    }

    /**
     * Method enumerateUuid.
     *
     * @return an Enumeration over all possible elements of this
     * collection
     */
    public java.util.Enumeration<String> enumerateUuid() {
        return java.util.Collections.enumeration(this._uuidList);
    }

    /**
     * Method getUuid.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.String at the given index
     */
    public String getUuid(final int index) throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._uuidList.size()) {
            throw new IndexOutOfBoundsException(
                    "getUuid: Index value '" + index + "' not in range [0.." + (this._uuidList.size() - 1) + "]");
        }

        return (String) _uuidList.get(index);
    }

    /**
     * Method getUuid.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     *
     * @return this collection as an Array
     */
    public String[] getUuid() {
        String[] array = new String[0];
        return (String[]) this._uuidList.toArray(array);
    }

    /**
     * Method getUuidCollection.Returns a reference to '_uuidList'.
     * No type checking is performed on any modifications to the
     * Vector.
     *
     * @return a reference to the Vector backing this class
     */
    public java.util.List<String> getUuidCollection() {
        return this._uuidList;
    }

    /**
     * Method getUuidCount.
     *
     * @return the size of this collection
     */
    public int getUuidCount() {
        return this._uuidList.size();
    }

    /**
     * Method iterateUuid.
     *
     * @return an Iterator over all possible elements in this
     * collection
     */
    public java.util.Iterator<String> iterateUuid() {
        return this._uuidList.iterator();
    }

    /**
     */
    public void removeAllUuid() {
        this._uuidList.clear();
    }

    /**
     * Method removeUuid.
     *
     * @param vUuid
     * @return true if the object was removed from the collection.
     */
    public boolean removeUuid(final String vUuid) {
        return _uuidList.remove(vUuid);
    }

    /**
     * Method removeUuidAt.
     *
     * @param index
     * @return the element removed from the collection
     */
    public String removeUuidAt(final int index) {
        return this._uuidList.remove(index);
    }

    /**
     *
     *
     * @param index
     * @param vUuid
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setUuid(final int index, final String vUuid) throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._uuidList.size()) {
            throw new IndexOutOfBoundsException(
                    "setUuid: Index value '" + index + "' not in range [0.." + (this._uuidList.size() - 1) + "]");
        }

        this._uuidList.set(index, vUuid);
    }

    /**
     * Sets the value of '_uuidList' by copying the given Vector.
     * All elements will be checked for type safety.
     *
     * @param vUuidList the Vector to copy.
     */
    public void setUuid(final java.util.List<String> vUuidList) {
        // copy vector
        this._uuidList.clear();

        this._uuidList.addAll(vUuidList);
    }

    /**
     * Sets the value of '_uuidList' by setting it to the given
     * Vector. No type checking is performed.
     * @deprecated
     *
     * @param uuidList the Vector to set.
     */
    public void setUuidCollection(final java.util.List<String> uuidList) {
        this._uuidList = uuidList;
    }

    @Override
    public String toString() {
        return new OnmsStringBuilder(this).toString();
    }
}
