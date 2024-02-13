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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This element is used for converting events into alerts.
 */
@XmlRootElement(name="alert-data")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("eventconf.xsd")
public class AlertData implements Serializable {
    private static final long serialVersionUID = 2L;

    @XmlAttribute(name="reduction-key", required=true)
    private String m_reductionKey;

    @XmlAttribute(name="alert-type")
    private Integer m_alertType;

    @XmlAttribute(name="clear-key")
    private String m_clearKey;

    @XmlAttribute(name="auto-clean")
    private Boolean m_autoClean;

    @XmlAttribute(name="x733-alert-type")
    private String m_x733AlertType;

    @XmlAttribute(name="x733-probable-cause")
    private Integer m_x733ProbableCause;

    @XmlElement(name="update-field", required=false)
    private List<UpdateField> m_updateFields = new ArrayList<>();

    @XmlElement(name="managed-object", required=false)
    private ManagedObject m_managedObject;


    public String getReductionKey() {
        return m_reductionKey;
    }

    public void setReductionKey(final String reductionKey) {
        m_reductionKey = ConfigUtils.normalizeAndInternString(reductionKey);
    }

    public Integer getAlertType() {
        return m_alertType;
    }

    public void setAlertType(final Integer alertType) {
        m_alertType = ConfigUtils.assertMinimumInclusive(ConfigUtils.assertNotNull(alertType, "alert-type"), 1, "alert-type");
    }

    public String getClearKey() {
        return m_clearKey;
    }

    public void setClearKey(final String clearKey) {
        m_clearKey = ConfigUtils.normalizeAndInternString(clearKey);
    }

    public Boolean getAutoClean() {
        return m_autoClean == null? Boolean.FALSE : m_autoClean; // XSD default is false
    }

    public void setAutoClean(final Boolean autoClean) {
        m_autoClean = autoClean;
    }

    public String getX733AlertType() {
        return m_x733AlertType;
    }

    public void setX733AlertType(final String x733AlertType) {
        m_x733AlertType = ConfigUtils.normalizeAndInternString(x733AlertType);
    }

    public Integer getX733ProbableCause() {
        return m_x733ProbableCause;
    }

    public void setX733ProbableCause(final Integer x733ProbableCause) {
        m_x733ProbableCause = x733ProbableCause;
    }

    public List<UpdateField> getUpdateFields() {
        return Collections.unmodifiableList(m_updateFields);
    }

    public void setUpdateFields(final List<UpdateField> updateFields) {
        if (m_updateFields == updateFields) return;
        m_updateFields.clear();
        if (updateFields != null) m_updateFields.addAll(updateFields);
    }

    public ManagedObject getManagedObject() {
        return m_managedObject;
    }

    public void setManagedObject(ManagedObject managedObject) {
        this.m_managedObject = managedObject;
    }
    @Override
    public int hashCode() {
        return Objects.hash(m_reductionKey,
                            m_alertType,
                            m_clearKey,
                            m_autoClean,
                            m_x733AlertType,
                            m_x733ProbableCause,
                            m_updateFields,
                            m_managedObject);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AlertData) {
            final AlertData that = (AlertData) obj;
            return Objects.equals(this.m_reductionKey, that.m_reductionKey) &&
                    Objects.equals(this.m_alertType, that.m_alertType) &&
                    Objects.equals(this.m_clearKey, that.m_clearKey) &&
                    Objects.equals(this.m_autoClean, that.m_autoClean) &&
                    Objects.equals(this.m_x733AlertType, that.m_x733AlertType) &&
                    Objects.equals(this.m_x733ProbableCause, that.m_x733ProbableCause) &&
                    Objects.equals(this.m_updateFields, that.m_updateFields) &&
                    Objects.equals(this.m_managedObject, that.m_managedObject);
        }
        return false;
    }


}
