/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.horizon.events.conf.xml;


import org.opennms.horizon.events.util.ConfigUtils;
import org.opennms.horizon.events.util.ValidateUsing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * Global settings for this configuration
 */
@XmlRootElement(name="global")
@XmlAccessorType(XmlAccessType.NONE)
@ValidateUsing("eventconf.xsd")
public class Global implements Serializable {
    private static final long serialVersionUID = 2L;
    /**
     * Security settings for this configuration
     */
    @XmlElement(name="security", required=true)
    private Security m_security;

    public Security getSecurity() {
        return m_security;
    }

    public void setSecurity(final Security security) {
        m_security = ConfigUtils.assertNotNull(security, "security");
    }

    public boolean isSecureTag(final String tag) {
        return m_security == null ? false : m_security.isSecureTag(tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_security);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Global) {
            final Global that = (Global) obj;
            return Objects.equals(this.m_security, that.m_security);
        }
        return false;
    }

}
