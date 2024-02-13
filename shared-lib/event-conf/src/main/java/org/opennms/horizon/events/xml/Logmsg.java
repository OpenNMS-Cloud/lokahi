/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.events.xml;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;

/**
 * The event logmsg with the destination attribute defining
 *  if event is for display only, logonly, log and display or
 * neither. A
 *  destination attribute of 'donotpersist' indicates that Eventd
 * is not to
 *  persist the event to the database. The optional notify
 * attributed can be 
 *  used to suppress notices on a particular event (by default it
 * is true - 
 *  i.e. a notice will be sent.
 * 
 * @version $Revision$ $Date$
 */

@XmlRootElement(name="logmsg")
@XmlAccessorType(XmlAccessType.FIELD)
//@ValidateUsing("event.xsd")
public class Logmsg implements Serializable {

    private static final long serialVersionUID = -7173862847984790914L;

    //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

	/**
     * internal content storage
     */
	@XmlValue
	@NotNull
    private String _content = "";

    /**
     * Field _notify.
     */
	@XmlAttribute(name="notify")
    private Boolean _notify = true;

    /**
     * Field _dest.
     */
    @XmlAttribute(name="dest")
    @Pattern(regexp="(logndisplay|displayonly|logonly|suppress|donotpersist)")
    private String _dest = "logndisplay";


      //----------------/
     //- Constructors -/
    //----------------/

    public Logmsg() {
        super();
        setContent("");
        setDest("logndisplay");
    }


      //-----------/
     //- Methods -/
    //-----------/

    /**
     */
    public void deleteNotify(
    ) {
        this._notify = null;
    }

    /**
     * Returns the value of field 'content'. The field 'content'
     * has the following description: internal content storage
     * 
     * @return the value of field 'Content'.
     */
    public String getContent(
    ) {
        return this._content;
    }

    /**
     * Returns the value of field 'dest'.
     * 
     * @return the value of field 'Dest'.
     */
    public String getDest(
    ) {
        return this._dest;
    }

    /**
     * Returns the value of field 'notify'.
     * 
     * @return the value of field 'Notify'.
     */
    public Boolean getNotify(
    ) {
        return this._notify == null? false : this._notify;
    }

    /**
     * Method hasNotify.
     * 
     * @return true if at least one Notify has been added
     */
    public boolean hasNotify(
    ) {
        return this._notify != null;
    }

    /**
     * Returns the value of field 'notify'.
     * 
     * @return the value of field 'Notify'.
     */
    public Boolean isNotify() {
        return getNotify();
    }

    /**
     * Sets the value of field 'content'. The field 'content' has
     * the following description: internal content storage
     * 
     * @param content the value of field 'content'.
     */
    public void setContent(
            final String content) {
        this._content = content;
    }

    /**
     * Sets the value of field 'dest'.
     * 
     * @param dest the value of field 'dest'.
     */
    public void setDest(
            final String dest) {
        this._dest = dest;
    }

    /**
     * Sets the value of field 'notify'.
     * 
     * @param notify the value of field 'notify'.
     */
    public void setNotify(
            final Boolean notify) {
        this._notify = notify;
    }

    @Override
    public String toString() {
    	return new OnmsStringBuilder(this).toString();
    }
}
