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


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * This element is used for converting events into alerts.
 * 
 */

@XmlRootElement(name="alert-data")
@XmlAccessorType(XmlAccessType.FIELD)
//@ValidateUsing("event.xsd")
public class AlertData implements Serializable {
	private static final long serialVersionUID = 3681502418413339216L;


    /**
     * Field _reductionKey.
     */
	@XmlAttribute(name="reduction-key", required=true)
	@NotNull
    private String _reductionKey;

    /**
     * Field _alertType.
     */
	@XmlAttribute(name="alert-type", required=true)
	@NotNull
	@Min(1)
    private Integer _alertType;

    /**
     * Field _clearKey.
     */
	@XmlAttribute(name="clear-key")
    private String _clearKey;

    /**
     * Field _autoClean.
     */
	@XmlAttribute(name="auto-clean")
    private Boolean _autoClean = false;

    /**
     * Field _x733AlertType.
     */
	@XmlAttribute(name="x733-alert-type")
    private String _x733AlertType;

    /**
     * Field _x733ProbableCause.
     */
	@XmlAttribute(name="x733-probable-cause")
    private Integer _x733ProbableCause;
	
	/**
	 * Field m_updateField
	 */
    @XmlElement(name="update-field", required=false)
    @Valid
    private List<UpdateField> m_updateFieldList = new ArrayList<>();

    /**
     * Field m_managedObject
     */
    @XmlElement(name="managed-object", required=false)
    private ManagedObject m_managedObject;


    public AlertData() {
        super();
    }



    /**
     */
    public void deleteAutoClean(
    ) {
        this._autoClean = null;
    }

    /**
     */
    public void deleteX733ProbableCause(
    ) {
        this._x733ProbableCause = null;
    }

    /**
     * Returns the value of field 'alertType'.
     * 
     * @return the value of field 'AlertType'.
     */
    public Integer getAlertType() {
        return this._alertType == null? 0 : this._alertType;
    }

    /**
     * Returns the value of field 'autoClean'.
     * 
     * @return the value of field 'AutoClean'.
     */
    public Boolean getAutoClean() {
        return this._autoClean == null? false : this._autoClean;
    }

    /**
     * Returns the value of field 'clearKey'.
     * 
     * @return the value of field 'ClearKey'.
     */
    public String getClearKey() {
        return this._clearKey;
    }

    /**
     * Returns the value of field 'reductionKey'.
     * 
     * @return the value of field 'ReductionKey'.
     */
    public String getReductionKey(
    ) {
        return this._reductionKey;
    }

    /**
     * Returns the value of field 'x733AlertType'.
     * 
     * @return the value of field 'X733AlertType'.
     */
    public String getX733AlertType(
    ) {
        return this._x733AlertType;
    }

    /**
     * Returns the value of field 'x733ProbableCause'.
     * 
     * @return the value of field 'X733ProbableCause'.
     */
    public Integer getX733ProbableCause() {
        return this._x733ProbableCause == null ? 0 : this._x733ProbableCause;
    }

    /**
     * Method hasAlertType.
     * 
     * @return true if at least one AlertType has been added
     */
    public boolean hasAlertType(
    ) {
        return this._alertType != null;
    }

    /**
     * Method hasAutoClean.
     * 
     * @return true if at least one AutoClean has been added
     */
    public boolean hasAutoClean(
    ) {
        return this._autoClean != null;
    }

    /**
     * Method hasX733ProbableCause.
     * 
     * @return true if at least one X733ProbableCause has been added
     */
    public boolean hasX733ProbableCause(
    ) {
        return this._x733ProbableCause != null;
    }

    /**
     * Returns the value of field 'autoClean'.
     * 
     * @return the value of field 'AutoClean'.
     */
    public Boolean isAutoClean(
    ) {
        return getAutoClean();
    }

    /**
     * Sets the value of field 'alertType'.
     * 
     * @param alertType the value of field 'alertType'.
     */
    public void setAlertType(
            final Integer alertType) {
        this._alertType = alertType;
    }

    /**
     * Sets the value of field 'autoClean'.
     * 
     * @param autoClean the value of field 'autoClean'.
     */
    public void setAutoClean(
            final Boolean autoClean) {
        this._autoClean = autoClean;
    }

    /**
     * Sets the value of field 'clearKey'.
     * 
     * @param clearKey the value of field 'clearKey'.
     */
    public void setClearKey(
            final String clearKey) {
        this._clearKey = clearKey;
    }

    /**
     * Sets the value of field 'reductionKey'.
     * 
     * @param reductionKey the value of field 'reductionKey'.
     */
    public void setReductionKey(
            final String reductionKey) {
        this._reductionKey = reductionKey;
    }

    /**
     * Sets the value of field 'x733AlertType'.
     * 
     * @param x733AlertType the value of field 'x733AlertType'.
     */
    public void setX733AlertType(
            final String x733AlertType) {
        this._x733AlertType = x733AlertType;
    }

    /**
     * Sets the value of field 'x733ProbableCause'.
     * 
     * @param x733ProbableCause the value of field
     * 'x733ProbableCause'.
     */
    public void setX733ProbableCause(
            final Integer x733ProbableCause) {
        this._x733ProbableCause = x733ProbableCause;
    }
    
    public UpdateField[] getUpdateField() {
        return m_updateFieldList.toArray(new UpdateField[0]);
    }
    
    public Collection<UpdateField> getUpdateFieldCollection() {
        return m_updateFieldList;
    }
    
    public List<UpdateField> getUpdateFieldList() {
        return m_updateFieldList;
    }
    
    public int getUpdateFieldListCount() {
        return m_updateFieldList.size();
    }

    public Boolean hasUpdateFields() {
        Boolean hasFields = true;
        if (m_updateFieldList == null || m_updateFieldList.isEmpty()) {
            hasFields = false;
        }
        return hasFields;
    }

    public void setUpdateField(final List<UpdateField> fields) {
        if (m_updateFieldList == fields) return;
        m_updateFieldList.clear();
        m_updateFieldList.addAll(fields);
    }
    
    public void setUpdateFieldCollection(final Collection<UpdateField> fields) {
        if (m_updateFieldList == fields) return;
        m_updateFieldList.clear();
        m_updateFieldList.addAll(fields);
    }


    public ManagedObject getManagedObject() {
        return m_managedObject;
    }

    public void setManagedObject(ManagedObject m_managedObject) {
        this.m_managedObject = m_managedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertData alertData = (AlertData) o;
        return Objects.equals(_reductionKey, alertData._reductionKey) &&
                Objects.equals(_alertType, alertData._alertType) &&
                Objects.equals(_clearKey, alertData._clearKey) &&
                Objects.equals(_autoClean, alertData._autoClean) &&
                Objects.equals(_x733AlertType, alertData._x733AlertType) &&
                Objects.equals(_x733ProbableCause, alertData._x733ProbableCause) &&
                Objects.equals(m_updateFieldList, alertData.m_updateFieldList) &&
                Objects.equals(m_managedObject, alertData.m_managedObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_reductionKey, _alertType, _clearKey, _autoClean, _x733AlertType, _x733ProbableCause, m_updateFieldList, m_managedObject);
    }

    @Override
    public String toString() {
    	return new OnmsStringBuilder(this).toString();
    }
}
