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
 * The forwarding information for this event - state
 *  determines if event is forwarded, mechanism determines how
 * event is
 *  forwarded .
 * 
 * @version $Revision$ $Date$
 */

@XmlRootElement(name="forward")
@XmlAccessorType(XmlAccessType.FIELD)
//@ValidateUsing("event.xsd")
public class Forward implements Serializable {
	private static final long serialVersionUID = -4795441559557516585L;

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
     * Field _state.
     */
	@XmlAttribute(name="state")
	@Pattern(regexp="(on|off)")
    private String _state = "off";

    /**
     * Field _mechanism.
     */
	@XmlAttribute(name="mechanism")
	@Pattern(regexp="(snmpudp|snmptcp|xmltcp|xmludp)")
    private String _mechanism = "snmpudp";


      //----------------/
     //- Constructors -/
    //----------------/

    public Forward() {
        super();
        setContent("");
        setState("off");
        setMechanism("snmpudp");
    }


      //-----------/
     //- Methods -/
    //-----------/

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
     * Returns the value of field 'mechanism'.
     * 
     * @return the value of field 'Mechanism'.
     */
    public String getMechanism(
    ) {
        return this._mechanism;
    }

    /**
     * Returns the value of field 'state'.
     * 
     * @return the value of field 'State'.
     */
    public String getState(
    ) {
        return this._state;
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
     * Sets the value of field 'mechanism'.
     * 
     * @param mechanism the value of field 'mechanism'.
     */
    public void setMechanism(
            final String mechanism) {
        this._mechanism = mechanism;
    }

    /**
     * Sets the value of field 'state'.
     * 
     * @param state the value of field 'state'.
     */
    public void setState(
            final String state) {
        this._state = state;
    }

        @Override
    public String toString() {
    	return new OnmsStringBuilder(this).toString();
    }
}
