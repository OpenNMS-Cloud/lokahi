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
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * The mask element
 *
 * @version $Revision$ $Date$
 */
@XmlRootElement(name = "maskelement")
@XmlAccessorType(XmlAccessType.FIELD)
// @ValidateUsing("event.xsd")
public class Maskelement implements Serializable {
    private static final long serialVersionUID = 6355834996920103487L;

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * The mask element name can only be one of those
     *  specified above
     */
    @XmlElement(name = "mename", required = true)
    @NotNull
    private String _mename;

    /**
     * The mask element value
     */
    @XmlElement(name = "mevalue", required = true)
    @Size(min = 1)
    private java.util.List<String> _mevalueList;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public Maskelement() {
        super();
        this._mevalueList = new java.util.ArrayList<>();
    }

    // -----------/
    // - Methods -/
    // -----------/

    /**
     *
     *
     * @param vMevalue
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addMevalue(final String vMevalue) throws IndexOutOfBoundsException {
        this._mevalueList.add(vMevalue.intern());
    }

    /**
     *
     *
     * @param index
     * @param vMevalue
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addMevalue(final int index, final String vMevalue) throws IndexOutOfBoundsException {
        this._mevalueList.add(index, vMevalue.intern());
    }

    /**
     * Method enumerateMevalue.
     *
     * @return an Enumeration over all possible elements of this
     * collection
     */
    public java.util.Enumeration<String> enumerateMevalue() {
        return java.util.Collections.enumeration(this._mevalueList);
    }

    /**
     * Returns the value of field 'mename'. The field 'mename' has
     * the following description: The mask element name can only be
     * one of those
     *  specified above
     *
     * @return the value of field 'Mename'.
     */
    public String getMename() {
        return this._mename;
    }

    /**
     * Method getMevalue.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the java.lang.String at the given index
     */
    public String getMevalue(final int index) throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._mevalueList.size()) {
            throw new IndexOutOfBoundsException(
                    "getMevalue: Index value '" + index + "' not in range [0.." + (this._mevalueList.size() - 1) + "]");
        }

        return (String) _mevalueList.get(index);
    }

    /**
     * Method getMevalue.Returns the contents of the collection in
     * an Array.  <p>Note:  Just in case the collection contents
     * are changing in another thread, we pass a 0-length Array of
     * the correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     *
     * @return this collection as an Array
     */
    public String[] getMevalue() {
        String[] array = new String[0];
        return (String[]) this._mevalueList.toArray(array);
    }

    /**
     * Method getMevalueCollection.Returns a reference to
     * '_mevalueList'. No type checking is performed on any
     * modifications to the Vector.
     *
     * @return a reference to the Vector backing this class
     */
    public java.util.List<String> getMevalueCollection() {
        return this._mevalueList;
    }

    /**
     * Method getMevalueCount.
     *
     * @return the size of this collection
     */
    public int getMevalueCount() {
        return this._mevalueList.size();
    }

    /**
     * Method iterateMevalue.
     *
     * @return an Iterator over all possible elements in this
     * collection
     */
    public java.util.Iterator<String> iterateMevalue() {
        return this._mevalueList.iterator();
    }

    /**
     */
    public void removeAllMevalue() {
        this._mevalueList.clear();
    }

    /**
     * Method removeMevalue.
     *
     * @param vMevalue
     * @return true if the object was removed from the collection.
     */
    public boolean removeMevalue(final String vMevalue) {
        boolean removed = _mevalueList.remove(vMevalue);
        return removed;
    }

    /**
     * Method removeMevalueAt.
     *
     * @param index
     * @return the element removed from the collection
     */
    public String removeMevalueAt(final int index) {
        Object obj = this._mevalueList.remove(index);
        return (String) obj;
    }

    /**
     * Sets the value of field 'mename'. The field 'mename' has the
     * following description: The mask element name can only be one
     * of those
     *  specified above
     *
     * @param mename the value of field 'mename'.
     */
    public void setMename(final String mename) {
        this._mename = mename.intern();
    }

    /**
     *
     *
     * @param index
     * @param vMevalue
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void setMevalue(final int index, final String vMevalue) throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._mevalueList.size()) {
            throw new IndexOutOfBoundsException(
                    "setMevalue: Index value '" + index + "' not in range [0.." + (this._mevalueList.size() - 1) + "]");
        }

        this._mevalueList.set(index, vMevalue.intern());
    }

    /**
     * Sets the value of '_mevalueList' by copying the given
     * Vector. All elements will be checked for type safety.
     *
     * @param vMevalueList the Vector to copy.
     */
    public void setMevalue(final java.util.List<String> vMevalueList) {
        // copy vector
        this._mevalueList.clear();
        for (final String value : vMevalueList) {
            this._mevalueList.add(value.intern());
        }
    }

    /**
     * Sets the value of '_mevalueList' by setting it to the given
     * Vector. No type checking is performed.
     * @deprecated
     *
     * @param mevalueList the Vector to set.
     */
    public void setMevalueCollection(final java.util.List<String> mevalueList) {
        this._mevalueList.clear();
        for (final String value : mevalueList) {
            this._mevalueList.add(value.intern());
        }
    }

    @Override
    public String toString() {
        return new OnmsStringBuilder(this).toString();
    }
}
