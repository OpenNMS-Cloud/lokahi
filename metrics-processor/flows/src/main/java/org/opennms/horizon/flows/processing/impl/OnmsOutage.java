/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing.impl;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

import com.google.common.base.MoreObjects;


public class OnmsOutage implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3846398168228820151L;

    /** identifier field */
    private Integer m_id;

    /** persistent field */
    private Date m_ifLostService;

    /** nullable persistent field */
    private Date m_ifRegainedService;

    /** persistent field */
    private OnmsEvent m_serviceRegainedEvent;

    /** persistent field */
    private OnmsEvent m_serviceLostEvent;

    /** persistent field */
    private OnmsMonitoredService m_monitoredService;
    
    /** persistent field */
    private Date m_suppressTime;
    
    /** persistent field */
    private String m_suppressedBy;

    /** persistent field */
    private OnmsMonitoringLocation m_perspective;

    /**
     * full constructor
     *
     * @param ifLostService a {@link Date} object.
     * @param ifRegainedService a {@link Date} object.
     * @param eventBySvcRegainedEvent a {@link OnmsEvent} object.
     * @param eventBySvcLostEvent a {@link OnmsEvent} object.
     * @param monitoredService a {@link OnmsMonitoredService} object.
     * @param suppressTime a {@link Date} object.
     * @param suppressedBy a {@link String} object.
     */
    public OnmsOutage(Date ifLostService, Date ifRegainedService, OnmsEvent eventBySvcRegainedEvent, OnmsEvent eventBySvcLostEvent, OnmsMonitoredService monitoredService, Date suppressTime, String suppressedBy) {
        m_ifLostService = ifLostService;
        m_ifRegainedService = ifRegainedService;
        m_serviceRegainedEvent = eventBySvcRegainedEvent;
        m_serviceLostEvent = eventBySvcLostEvent;
        m_monitoredService = monitoredService;
        m_suppressTime = suppressTime;
        m_suppressedBy = suppressedBy;
        
    }

    /**
     * default constructor
     */
    public OnmsOutage() {
    }

    /**
     */
    public OnmsOutage(Date ifLostService, OnmsMonitoredService monitoredService) {
        m_ifLostService = ifLostService;
        m_monitoredService = monitoredService;
    }

    public OnmsOutage(Date ifLostService, Date ifRegainedService, OnmsMonitoredService monitoredService) {
        m_ifLostService = ifLostService;
        m_ifRegainedService = ifRegainedService;
        m_monitoredService = monitoredService;
    }

    /**
     * minimal constructor
     *
     * @param ifLostService a {@link Date} object.
     * @param eventBySvcLostEvent a {@link OnmsEvent} object.
     * @param monitoredService a {@link OnmsMonitoredService} object.
     */
    public OnmsOutage(Date ifLostService, OnmsEvent eventBySvcLostEvent, OnmsMonitoredService monitoredService) {
        m_ifLostService = ifLostService;
        m_serviceLostEvent = eventBySvcLostEvent;
        m_monitoredService = monitoredService;
    }

    /**
     * <p>getId</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getId() {
        return m_id;
    }

    /**
     * <p>setId</p>
     *
     * @param outageId a {@link Integer} object.
     */
    public void setId(Integer outageId) {
        m_id = outageId;
    }

    // @XmlTransient
    /**
     * <p>getMonitoredService</p>
     *
     * @return a {@link OnmsMonitoredService} object.
     */
    public OnmsMonitoredService getMonitoredService() {
        return m_monitoredService;
    }

    /**
     * <p>setMonitoredService</p>
     *
     * @param monitoredService a {@link OnmsMonitoredService} object.
     */
    public void setMonitoredService(OnmsMonitoredService monitoredService) {
        m_monitoredService = monitoredService;
    }

    
    /**
     * <p>getIfLostService</p>
     *
     * @return a {@link Date} object.
     */
    public Date getIfLostService() {
        return m_ifLostService;
    }

    /**
     * <p>setIfLostService</p>
     *
     * @param ifLostService a {@link Date} object.
     */
    public void setIfLostService(Date ifLostService) {
        m_ifLostService = ifLostService;
    }

    /**
     * <p>getServiceLostEvent</p>
     *
     * @return a {@link OnmsEvent} object.
     */
    public OnmsEvent getServiceLostEvent() {
        return m_serviceLostEvent;
    }

    /**
     * <p>setServiceLostEvent</p>
     *
     * @param svcLostEvent a {@link OnmsEvent} object.
     */
    public void setServiceLostEvent(OnmsEvent svcLostEvent) {
        m_serviceLostEvent = svcLostEvent;
    }


    /**
     * <p>getIfRegainedService</p>
     *
     * @return a {@link Date} object.
     */

    public Date getIfRegainedService() {
        return m_ifRegainedService;
    }
    
    /**
     * <p>setIfRegainedService</p>
     *
     * @param ifRegainedService a {@link Date} object.
     */
    public void setIfRegainedService(Date ifRegainedService) {
        m_ifRegainedService = ifRegainedService;
    }

    /**
     * <p>getServiceRegainedEvent</p>
     *
     * @return a {@link OnmsEvent} object.
     */
    public OnmsEvent getServiceRegainedEvent() {
        return m_serviceRegainedEvent;
    }

    /**
     * <p>setServiceRegainedEvent</p>
     *
     * @param svcRegainedEvent a {@link OnmsEvent} object.
     */
    public void setServiceRegainedEvent(OnmsEvent svcRegainedEvent) {
        m_serviceRegainedEvent = svcRegainedEvent;
    }

    /**
     * <p>getSuppressTime</p>
     *
     * @return a {@link Date} object.
     */
    public Date getSuppressTime(){
    	return m_suppressTime;
    }
    
    /**
     * <p>setSuppressTime</p>
     *
     * @param timeToSuppress a {@link Date} object.
     */
    public void setSuppressTime(Date timeToSuppress){
    	m_suppressTime = timeToSuppress;
    }
    
    
    /**
     * <p>getSuppressedBy</p>
     *
     * @return a {@link String} object.
     */
    public String getSuppressedBy(){
    	return m_suppressedBy;
    }
    
    /**
     * <p>setSuppressedBy</p>
     *
     * @param suppressorMan a {@link String} object.
     */
    public void setSuppressedBy(String suppressorMan){
    	m_suppressedBy = suppressorMan;
    }


    /**
     * This method is necessary for CXF to be able to introspect
     * the type of {@link OnmsNode} parameters.
     *
     * @return a {@link OnmsNode} object.
     */
    public OnmsNode getNode() {
        return getMonitoredService().getIpInterface().getNode();
    }

    /**
     * This method is necessary for CXF to be able to introspect
     * the type of {@link OnmsNode} parameters.
     */
    public void setNode(OnmsNode node) {
        OnmsMonitoredService service = getMonitoredService();
        if (service == null) {
            service = new OnmsMonitoredService();
            setMonitoredService(service);
        }
        OnmsIpInterface intf = service.getIpInterface();
        if (intf == null) {
            intf = new OnmsIpInterface();
            service.setIpInterface(intf);
        }
        intf.setNode(node);
    }

    /**
     * <p>getNodeId</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getNodeId(){
    	return getMonitoredService().getNodeId();
    }

    /**
     * <p>getNodeLabel</p>
     *
     * @return a {@link String} object.
     */
    public String getNodeLabel(){
        return getMonitoredService().getIpInterface().getNode().getLabel();
    }

    /**
     * <p>getForeignSource</p>
     *
     * @return a {@link String} object.
     */
    public String getForeignSource(){
        return getMonitoredService().getIpInterface().getNode().getForeignSource();
    }

    /**
     * <p>getForeignId</p>
     *
     * @return a {@link String} object.
     */
    public String getForeignId(){
        return getMonitoredService().getIpInterface().getNode().getForeignId();
    }

    /**
     * <p>getLocationName</p>
     *
     * @return a {@link String} object.
     */
    public String getLocationName(){
        return getMonitoredService().getIpInterface().getNode().getLocation().getLocationName();
    }

    /**
     * <p>getIpAddress</p>
     *
     * @return a {@link String} object.
     */
    public InetAddress getIpAddress() {
        return getMonitoredService().getIpAddress();
    }

    /**
     * <p>getIpAddressAsString</p>
     *
     * @return a {@link String} object.
     * @deprecated use getIpAddress
     */
    public String getIpAddressAsString() {
        return getMonitoredService().getIpAddressAsString();
    }

    /**
     * <p>getServiceId</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getServiceId() {
    	return getMonitoredService().getServiceId();
    }

    /**
     * This method is necessary for CXF to be able to introspect
     * the type of {@link OnmsNode} parameters.
     *
     * @return a {@link OnmsServiceType} object.
     */
    public OnmsServiceType getServiceType() {
        return getMonitoredService().getServiceType();
    }

    /**
     * This method is necessary for CXF to be able to introspect
     * the type of {@link OnmsServiceType} parameters.
     */
    public void setServiceType(OnmsServiceType type) {
        OnmsMonitoredService service = getMonitoredService();
        if (service == null) {
            service = new OnmsMonitoredService();
            setMonitoredService(service);
        }
        service.setServiceType(type);
    }

    /**
     * Monitoring perspective that this outage is associated with.
     */
    public OnmsMonitoringLocation getPerspective() {
        return m_perspective;
    }

    /**
     * Set the monitoring perspective for this outage.
     */
    public void setPerspective(OnmsMonitoringLocation perspective) {
        m_perspective = perspective;
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link String} object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("outageId", m_id)
            .add("ifLostService", m_ifLostService)
            .add("ifRegainedService", m_ifRegainedService)
            .add("ifRegainedServiceEvent", m_serviceRegainedEvent)
            .add("service", m_monitoredService)
            .add("suppressedBy", m_suppressedBy)
            .add("suppressTime", m_suppressTime)
            .add("perspective", m_perspective)
            .toString();
    }

}
