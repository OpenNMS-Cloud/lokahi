/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

import static org.opennms.horizon.shared.utils.InetAddressUtils.toInteger;
import static org.opennms.horizon.flows.processing.impl.OnmsEntity.hasNewValue;

import java.beans.Transient;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


import com.google.common.base.MoreObjects;

public class OnmsMonitoredService implements Comparable<OnmsMonitoredService> {
    private Integer m_id;

    private Date m_lastGood;

    private Date m_lastFail;

    private String m_qualifier;

    private String m_status;

    private String m_source;

    private String m_notify;

    private OnmsServiceType m_serviceType;

    private OnmsIpInterface m_ipInterface;

    /*
     * This is a set only because we want it to be lazy
     * and we need a better query language (i.e. HQL)
     * to make this work.  In this case, the Set size
     * will always be 1 or empty because there can only
     * be one outage at a time on a service.
     * 
     * With distributed monitoring, there will probably
     * be a model change were one service can be represented
     * by more than one outage.
     */
    private Set<OnmsOutage> m_currentOutages = new LinkedHashSet<>();

    private Set<OnmsApplication> m_applications = new LinkedHashSet<>();

    private List<OnmsMetaData> m_metaData = new ArrayList<>();

    public static final Map<String, String> STATUS_MAP;

    static {
        STATUS_MAP = new HashMap<String, String>();
        STATUS_MAP.put("A", "Managed");
        STATUS_MAP.put("U", "Unmanaged");
        STATUS_MAP.put("D", "Deleted");
        STATUS_MAP.put("F", "Forced Unmanaged");
        STATUS_MAP.put("N", "Not Monitored");
        STATUS_MAP.put("R", "Rescan to Resume");
        STATUS_MAP.put("S", "Rescan to Suspend");
        STATUS_MAP.put("X", "Remotely Monitored");
	}

    /**
     * <p>Constructor for OnmsMonitoredService.</p>
     */
    public OnmsMonitoredService() {
    }

    /**
     * <p>Constructor for OnmsMonitoredService.</p>
     *
     * @param ipIf a {@link OnmsIpInterface} object.
     * @param serviceType a {@link OnmsServiceType} object.
     */
    public OnmsMonitoredService(OnmsIpInterface ipIf, OnmsServiceType serviceType) {
        m_ipInterface = ipIf;
        m_ipInterface.getMonitoredServices().add(this);
        m_serviceType = serviceType;

    }

    /**
     * Unique identifier for ifService.
     *
     * @return a {@link Integer} object.
     */
    public Integer getId() {
        return m_id;
    }

    @Transient
    public Integer getJsonId() {
        return m_id;
    }
    /**
     * <p>setId</p>
     *
     * @param id a {@link Integer} object.
     */
    public void setId(Integer id) {
        m_id = id;
    }

    /**
     * This id is used for the serialized representation such as json, xml etc.
     */
    public String getXmlId() {
        return getId() == null? null : getId().toString();
    }

    public void setXmlId(final String id) {
        setId(Integer.valueOf(id));
    }

    /**
     * <p>getIpAddress</p>
     *
     * @return a {@link String} object.
     */
    public InetAddress getIpAddress() {
        return m_ipInterface.getIpAddress();
    }

    /**
     * <p>getIpAddress</p>
     *
     * @return a {@link String} object.
     * 
     * @deprecated
     */

    public String getIpAddressAsString() {
        return m_ipInterface.getIpAddressAsString();
    }

    /**
     * <p>getIfIndex</p>
     *
     * @return a {@link Integer} object.
     */

    public Integer getIfIndex() {
        return m_ipInterface.getIfIndex();
    }

    /**
     * <p>getLastGood</p>
     *
     * @return a {@link Date} object.
     */
    public Date getLastGood() {
        return m_lastGood;
    }

    /**
     * <p>setLastGood</p>
     *
     * @param lastgood a {@link Date} object.
     */
    public void setLastGood(Date lastgood) {
        m_lastGood = lastgood;
    }

    /**
     * <p>getLastFail</p>
     *
     * @return a {@link Date} object.
     */
    public Date getLastFail() {
        return m_lastFail;
    }

    /**
     * <p>setLastFail</p>
     *
     * @param lastfail a {@link Date} object.
     */
    public void setLastFail(Date lastfail) {
        m_lastFail = lastfail;
    }

    /**
     * <p>getQualifier</p>
     *
     * @return a {@link String} object.
     */
    public String getQualifier() {
        return m_qualifier;
    }

    /**
     * <p>setQualifier</p>
     *
     * @param qualifier a {@link String} object.
     */
    public void setQualifier(String qualifier) {
        m_qualifier = qualifier;
    }

    /**
     * <p>getStatus</p>
     *
     * @return a {@link String} object.
     */
    public String getStatus() {
        return m_status;
    }

    /**
     * <p>setStatus</p>
     *
     * @param status a {@link String} object.
     */
    public void setStatus(String status) {
        m_status = status;
    }
    
    @Transient
    public String getStatusLong() {
    	return STATUS_MAP.get(getStatus());
    }

    /**
     * <p>getSource</p>
     *
     * @return a {@link String} object.
     */
    public String getSource() {
        return m_source;
    }

    /**
     * <p>setSource</p>
     *
     * @param source a {@link String} object.
     */
    public void setSource(String source) {
        m_source = source;
    }

    /**
     * <p>getNotify</p>
     *
     * @return a {@link String} object.
     */
    public String getNotify() {
        return m_notify;
    }

    /**
     * <p>setNotify</p>
     *
     * @param notify a {@link String} object.
     */
    public void setNotify(String notify) {
        m_notify = notify;
    }

    public List<OnmsMetaData> getMetaData() {
        return m_metaData;
    }

    public void setMetaData(final List<OnmsMetaData> metaData) {
        m_metaData = metaData;
    }

    public void addMetaData(final String context, final String key, final String value) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        final Optional<OnmsMetaData> entry = getMetaData().stream()
                .filter(m -> m.getContext().equals(context))
                .filter(m -> m.getKey().equals(key))
                .findFirst();

        // Update the value if present, otherwise create a new entry
        if (entry.isPresent()) {
            entry.get().setValue(value);
        } else {
            getMetaData().add(new OnmsMetaData(context, key, value));
        }
    }

    public void removeMetaData(final String context, final String key) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(key);
        final Iterator<OnmsMetaData> iterator = getMetaData().iterator();

        while (iterator.hasNext()) {
            final OnmsMetaData onmsNodeMetaData = iterator.next();

            if (context.equals(onmsNodeMetaData.getContext()) && key.equals(onmsNodeMetaData.getKey())) {
                iterator.remove();
            }
        }
    }

    public void removeMetaData(final String context) {
        Objects.requireNonNull(context);
        final Iterator<OnmsMetaData> iterator = getMetaData().iterator();

        while (iterator.hasNext()) {
            final OnmsMetaData onmsNodeMetaData = iterator.next();

            if (context.equals(onmsNodeMetaData.getContext())) {
                iterator.remove();
            }
        }
    }

    /**
     * <p>getIpInterface</p>
     *
     * @return a {@link OnmsIpInterface} object.
     */
    public OnmsIpInterface getIpInterface() {
        return m_ipInterface;
    }


    @Transient
    public Integer getIpInterfaceId() {
        return m_ipInterface.getId();
    }

    /**
     * <p>setIpInterface</p>
     *
     * @param ipInterface a {@link OnmsIpInterface} object.
     */
    public void setIpInterface(OnmsIpInterface ipInterface) {
        m_ipInterface = ipInterface;
    }

    /**
     * <p>getNodeId</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getNodeId() {
        return m_ipInterface.getNode().getId();
    }

    /**
     * <p>getServiceType</p>
     *
     * @return a {@link OnmsServiceType} object.
     */
    public OnmsServiceType getServiceType() {
        return m_serviceType;
    }

    /**
     * <p>setServiceType</p>
     *
     * @param service a {@link OnmsServiceType} object.
     */
    public void setServiceType(OnmsServiceType service) {
        m_serviceType = service;
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link String} object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
        .add("id", m_id)
        .add("lastGood", m_lastGood)
        .add("lastFail", m_lastFail)
        .add("qualifier", m_qualifier)
        .add("status", m_status)
        .add("source", m_source)
        .add("notify", m_notify)
        .add("serviceType", m_serviceType)
        // cannot include these since the require db queries
//        .add("ipInterface", m_ipInterface)
//        .add("currentOutages", m_currentOutages)
//        .add("applications", m_applications)
        .toString();
    }

    /**
     * <p>getServiceId</p>
     *
     * @return a {@link Integer} object.
     */
    @Transient
    public Integer getServiceId() {
        return getServiceType().getId();
    }

    /** {@inheritDoc} */
    public void visit(EntityVisitor visitor) {
        visitor.visitMonitoredService(this);
        visitor.visitMonitoredServiceComplete(this);
    }


    public String getServiceName() {
        return getServiceType().getName();
    }


    public boolean isDown() {
        boolean down = true;
        if (!"A".equals(getStatus()) || m_currentOutages.isEmpty()) {
            return !down;
        }

        return down;
    }


    public Set<OnmsOutage> getCurrentOutages() {
        return m_currentOutages;
    }

    /**
     * <p>setCurrentOutages</p>
     *
     * @param currentOutages a {@link Set} object.
     */
    public void setCurrentOutages(Set<OnmsOutage> currentOutages) {
        m_currentOutages = currentOutages;
    }

    public Set<OnmsApplication> getApplications() {
        return m_applications;
    }

    /**
     * <p>setApplications</p>
     *
     * @param applications a {@link Set} object.
     */
    public void setApplications(Set<OnmsApplication> applications) {
        m_applications = applications;
    }

    /**
     * <p>addApplication</p>
     *
     * @param application a {@link OnmsApplication} object.
     * @return a boolean.
     */
    public boolean addApplication(OnmsApplication application) {
        return getApplications().add(application);
    }

    /**
     * <p>removeApplication</p>
     *
     * @param application a {@link OnmsApplication} object.
     * @return a boolean.
     */
    public boolean removeApplication(OnmsApplication application) {
        return getApplications().remove(application);
    }

    /**
     * <p>compareTo</p>
     *
     * @param o a {@link OnmsMonitoredService} object.
     * @return a int.
     */
    @Override
    public int compareTo(OnmsMonitoredService o) {
        int diff;

        diff = getIpInterface().getNode().getLabel().compareToIgnoreCase(o.getIpInterface().getNode().getLabel());
        if (diff != 0) {
            return diff;
        }

        BigInteger a = toInteger(getIpAddress());
        BigInteger b = toInteger(o.getIpAddress());
        diff = a.compareTo(b);
        if (diff != 0) {
            return diff;
        }

        return getServiceName().compareToIgnoreCase(o.getServiceName());
    }

    /**
     * <p>mergeServiceAttributes</p>
     *
     * @param scanned a {@link OnmsMonitoredService} object.
     */
    public void mergeServiceAttributes(OnmsMonitoredService scanned) {

        if (hasNewValue(scanned.getQualifier(), getQualifier())) {
            setQualifier(scanned.getQualifier());
        }

        if (hasNewStatusValue(scanned.getStatus(), getStatus())) {
            setStatus(scanned.getStatus());
        }
        
        if (hasNewValue(scanned.getSource(), getSource())) {
            setSource(scanned.getSource());
        }

        if (hasNewValue(scanned.getNotify(), getNotify())) {
            setNotify(scanned.getNotify());
        }

    }

    public void mergeMetaData(OnmsMonitoredService scanned) {
        if (!getMetaData().equals(scanned.getMetaData())) {
            setMetaData(scanned.getMetaData());
        }
    }

	private boolean hasNewStatusValue(String newStatus, String oldStatus) 
	{
		/*
		 * Don't overwrite the 'Not Monitored' in the database when provisioning the
		 * node.  The Poller will update it when scheduling it packages.
		 */
		return !"N".equals(oldStatus) && newStatus != null && !newStatus.equals(oldStatus);
	}

    public String getForeignSource() {
        if (getIpInterface() != null) {
            return getIpInterface().getForeignSource();
        }
        return null;
    }

    public String getForeignId() {
        if (getIpInterface() != null) {
            return getIpInterface().getForeignId();
        }
        return null;
    }
}
