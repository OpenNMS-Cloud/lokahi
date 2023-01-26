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
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;

/**
 * parm value
 * 
 * @version $Revision$ $Date$
 */

@XmlRootElement(name="value")
@XmlAccessorType(XmlAccessType.FIELD)
//@ValidateUsing("event.xsd")
public class Value implements Serializable {
	private static final long serialVersionUID = 6267247580169994541L;

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
     * Field _type.
     */
	@XmlAttribute(name="type")
	@Pattern(regexp="(int|string|Int32|OctetString|Null|ObjectIdentifier|Sequence|IpAddress|Counter32|Gauge32|TimeTicks|Opaque|Counter64|json)")
    private String _type = "string";

    /**
     * Field _encoding.
     */
	@XmlAttribute(name="encoding")
	@Pattern(regexp="(text|base64)")
    private String _encoding = "text";

	@XmlTransient
    private Boolean _expand = Boolean.FALSE;


      //----------------/
     //- Constructors -/
    //----------------/

    public Value() {
        super();
        setContent("");
        setType("string");
        setEncoding("text");
    }


      //-----------/
     //- Methods -/
    //-----------/

    public Value(final String value) {
    	this();
    	setContent(value);
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
     * Returns the value of field 'encoding'.
     * 
     * @return the value of field 'Encoding'.
     */
    public String getEncoding(
    ) {
        return this._encoding;
    }

    /**
     * Returns the value of field 'type'.
     * 
     * @return the value of field 'Type'.
     */
    public String getType(
    ) {
        return this._type;
    }

    /**
     * Returns the value of field 'expand'.
     * 
     * @return the value of field 'Expand'.
     */
    public Boolean isExpand(
    ) {
        return this._expand;
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
     * Sets the value of field 'encoding'.
     * 
     * @param encoding the value of field 'encoding'.
     */
    public void setEncoding(
            final String encoding) {
        this._encoding = encoding;
    }

    /**
     * Sets the value of field 'type'.
     * 
     * @param type the value of field 'type'.
     */
    public void setType(
            final String type) {
        this._type = type;
    }

    /**
     * Sets the value of field 'expand'.
     * 
     * @param expand, type the value of field 'expand'.
     */
    public void setExpand(
            final Boolean expand) {
        this._expand = expand;
    }

        @Override
    public String toString() {
    	return new OnmsStringBuilder(this).toString();
    }
}
