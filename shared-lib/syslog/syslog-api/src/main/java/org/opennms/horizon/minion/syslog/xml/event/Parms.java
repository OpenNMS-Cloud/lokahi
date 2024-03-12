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
package org.opennms.horizon.minion.syslog.xml.event;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import io.swagger.v3.oas.annotations.Hidden;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * The varbinds from the trap
 * 
 * @version $Revision$ $Date$
 */

@XmlRootElement(name="parms")
@XmlAccessorType(XmlAccessType.FIELD)
@Deprecated
//@ValidateUsing("event.xsd")
public class Parms implements Serializable {
	private static final long serialVersionUID = 1361348948961582446L;

	//--------------------------/
	//- Class/Member Variables -/
    //--------------------------/

    /**
     * A varbind from the trap
     */
	@XmlElement(name="parm", required=true)
	@Size(min=1)
	@Valid
    private java.util.List<Parm> _parmList;


      //----------------/
     //- Constructors -/
    //----------------/

    public Parms() {
        super();
        this._parmList = new java.util.ArrayList<>();
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * 
     * 
     * @param vParm
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addParm(
            final Parm vParm)
    throws IndexOutOfBoundsException {
        this._parmList.add(vParm);
    }

    /**
     * 
     * 
     * @param index
     * @param vParm
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    public void addParm(
            final int index,
            final Parm vParm)
    throws IndexOutOfBoundsException {
        this._parmList.add(index, vParm);
    }

    /**
     * Method enumerateParm.
     * 
     * @return an Enumeration over all possible elements of this
     * collection
     */
    public java.util.Enumeration<Parm> enumerateParm(
    ) {
        return java.util.Collections.enumeration(this._parmList);
    }

    /**
     * Method getParm.
     * 
     * @param index
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     * @return the value of the org.opennms.netmgt.xml.event.Parm
     * at the given index
     */
    public Parm getParm(
            final int index)
    throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._parmList.size()) {
            throw new IndexOutOfBoundsException("getParm: Index value '" + index + "' not in range [0.." + (this._parmList.size() - 1) + "]");
        }
        
        return (Parm) _parmList.get(index);
    }

    /**
     * Method getParm.Returns the contents of the collection in an
     * Array.  <p>Note:  Just in case the collection contents are
     * changing in another thread, we pass a 0-length Array of the
     * correct type into the API call.  This way we <i>know</i>
     * that the Array returned is of exactly the correct length.
     * 
     * @return this collection as an Array
     */
    public Parm[] getParm(
    ) {
        Parm[] array = new Parm[0];
        return (Parm[]) this._parmList.toArray(array);
    }

    /**
     * Method getParmCollection.Returns a reference to '_parmList'.
     * No type checking is performed on any modifications to the
     * Vector.
     * 
     * @deprecated This entire class has been deprecated. Use Event.getParmCollection instead
     * 
     * 
     * @return a reference to the Vector backing this class
     */
    public java.util.List<Parm> getParmCollection(
    ) {
        return this._parmList;
    }

    /**
     * Method getParmCount.
     * 
     * @return the size of this collection
     */
    public int getParmCount(
    ) {
        return this._parmList.size();
    }

    /**
     * Method iterateParm.
     * 
     * @return an Iterator over all possible elements in this
     * collection
     */
    public java.util.Iterator<Parm> iterateParm(
    ) {
        return this._parmList.iterator();
    }

    /**
     */
    public void removeAllParm(
    ) {
        this._parmList.clear();
    }

    /**
     * Method removeParm.
     * 
     * @param vParm
     * @return true if the object was removed from the collection.
     */
    public boolean removeParm(
            final Parm vParm) {
        boolean removed = _parmList.remove(vParm);
        return removed;
    }

    /**
     * Method removeParmAt.
     * 
     * @param index
     * @return the element removed from the collection
     */
    public Parm removeParmAt(
            final int index) {
        Object obj = this._parmList.remove(index);
        return (Parm) obj;
    }

    /**
     * 
     * @deprecated
     * @param index
     * @param vParm
     * @throws IndexOutOfBoundsException if the index
     * given is outside the bounds of the collection
     */
    @Hidden
    public void setParm(
            final int index,
            final Parm vParm)
    throws IndexOutOfBoundsException {
        // check bounds for index
        if (index < 0 || index >= this._parmList.size()) {
            throw new IndexOutOfBoundsException("setParm: Index value '" + index + "' not in range [0.." + (this._parmList.size() - 1) + "]");
        }
        
        this._parmList.set(index, vParm);
    }

    /**
     * 
     * @deprecated
     * @param vParmArray
     */
    @Hidden
    public void setParm(
            final Parm[] vParmArray) {
        //-- copy array
        _parmList.clear();
        
        for (int i = 0; i < vParmArray.length; i++) {
                this._parmList.add(vParmArray[i]);
        }
    }

    /**
     * Sets the value of '_parmList' by copying the given Vector.
     * All elements will be checked for type safety.
     * 
     * @param vParmList the Vector to copy.
     */
    public void setParm(
            final java.util.List<Parm> vParmList) {
        // copy vector
        this._parmList.clear();
        
        this._parmList.addAll(vParmList);
    }

    /**
     * Sets the value of '_parmList' by setting it to the given
     * Vector. No type checking is performed.
     * @deprecated
     * 
     * @param parmList the Vector to set.
     */
    public void setParmCollection(
            final java.util.List<Parm> parmList) {
        this._parmList = parmList;
    }

        @Override
    public String toString() {
    	return new OnmsStringBuilder(this).toString();
    }
}
