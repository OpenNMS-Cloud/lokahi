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

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import com.google.common.base.MoreObjects;

public class OnmsEvent extends OnmsEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -7412025003474162992L;

    private Integer eventId;

    private String eventUei;

    private Date eventTime;

    private String eventHost;

    private String eventSource;

    private InetAddress ipAddr;

    private OnmsMonitoringSystem distPoller;

    private String eventSnmpHost;

    private OnmsServiceType serviceType;

    private String eventSnmp;

    private List<OnmsEventParameter> eventParameters;

    private Date eventCreateTime;

    private String eventDescr;

    private String eventLogGroup;

    private String eventLogMsg;

    private Integer eventSeverity;

    private Integer ifIndex;

    private String eventPathOutage;

    private String eventCorrelation;

    private Integer eventSuppressedCount;

    private String eventOperInstruct;

    private String eventAutoAction;

    private String eventOperAction;

    private String eventOperActionMenuText;

    private String eventNotification;

    private String eventTTicket;

    private Integer eventTTicketState;

    private String eventForward;

    private String eventMouseOverText;

    private String eventLog;

    private String eventDisplay;

    private String eventAckUser;

    /**
     * nullable persistent field
     */
    private Date eventAckTime;

    /**
     * persistent field
     */
    private OnmsNode node;

    /**
     * persistent field
     */
    private Set<OnmsNotification> notifications = new HashSet<>();

    /**
     * persistent field
     */
    private Set<OnmsOutage> associatedServiceRegainedOutages = new HashSet<>();

    /**
     * persistent field
     */
    private Set<OnmsOutage> associatedServiceLostOutages = new HashSet<>();

    /**
     * default constructor
     */
    public OnmsEvent() {
    }

    /**
     * <p>getId</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getId() {
        return eventId;
    }

    /**
     * <p>setId</p>
     *
     * @param eventid a {@link Integer} object.
     */
    public void setId(Integer eventid) {
        eventId = eventid;
    }

    /**
     * <p>getEventUei</p>
     *
     * @return a {@link String} object.
     */
    public String getEventUei() {
        return eventUei;
    }

    /**
     * <p>setEventUei</p>
     *
     * @param eventuei a {@link String} object.
     */
    public void setEventUei(String eventuei) {
        eventUei = eventuei;
    }

    /**
     * <p>getEventTime</p>
     *
     * @return a {@link Date} object.
     */
    public Date getEventTime() {
        return eventTime;
    }

    /**
     * <p>setEventTime</p>
     *
     * @param eventtime a {@link Date} object.
     */
    public void setEventTime(Date eventtime) {
        eventTime = eventtime;
    }

    /**
     * <p>getEventHost</p>
     *
     * @return a {@link String} object.
     */
    public String getEventHost() {
        return eventHost;
    }

    /**
     * <p>setEventHost</p>
     *
     * @param eventhost a {@link String} object.
     */
    public void setEventHost(String eventhost) {
        eventHost = eventhost;
    }

    /**
     * <p>getEventSource</p>
     *
     * @return a {@link String} object.
     */
    public String getEventSource() {
        return eventSource;
    }

    /**
     * <p>setEventSource</p>
     *
     * @param eventsource a {@link String} object.
     */
    public void setEventSource(String eventsource) {
        eventSource = eventsource;
    }

    /**
     * <p>getIpAddr</p>
     *
     * @return a {@link String} object.
     */
    public InetAddress getIpAddr() {
        return ipAddr;
    }

    /**
     * <p>setIpAddr</p>
     *
     * @param ipaddr a {@link String} object.
     */
    public void setIpAddr(InetAddress ipaddr) {
        ipAddr = ipaddr;
    }

    /**
     * <p>getDistPoller</p>
     *
     * @return a {@link OnmsMonitoringSystem} object.
     */
    public OnmsMonitoringSystem getDistPoller() {
        return distPoller;
    }

    /**
     * <p>setDistPoller</p>
     *
     * @param distPoller a {@link OnmsDistPoller} object.
     */
    public void setDistPoller(OnmsMonitoringSystem distPoller) {
        this.distPoller = distPoller;
    }

    /**
     * <p>getEventSnmpHost</p>
     *
     * @return a {@link String} object.
     */
    public String getEventSnmpHost() {
        return eventSnmpHost;
    }

    /**
     * <p>setEventSnmpHost</p>
     *
     * @param eventsnmphost a {@link String} object.
     */
    public void setEventSnmpHost(String eventsnmphost) {
        eventSnmpHost = eventsnmphost;
    }

    /**
     * <p>getServiceType</p>
     *
     * @return a {@link OnmsServiceType} object.
     */
    public OnmsServiceType getServiceType() {
        return serviceType;
    }

    /**
     * <p>setServiceType</p>
     *
     * @param serviceType a {@link OnmsServiceType} object.
     */
    public void setServiceType(OnmsServiceType serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * <p>getEventSnmp</p>
     *
     * @return a {@link String} object.
     */
    public String getEventSnmp() {
        return eventSnmp;
    }

    /**
     * <p>setEventSnmp</p>
     *
     * @param eventsnmp a {@link String} object.
     */
    public void setEventSnmp(String eventsnmp) {
        eventSnmp = eventsnmp;
    }

    public List<OnmsEventParameter> getEventParameters() {
        if (this.eventParameters != null) {
            this.eventParameters.sort(Comparator.comparing(OnmsEventParameter::getPosition));
        }
        return this.eventParameters;
    }

    public void setEventParameters(List<OnmsEventParameter> eventParameters) {
        this.eventParameters = eventParameters;
        setPositionsOnParameters(this.eventParameters);
    }

    public void setEventParametersFromEvent(final Event event) {
        this.eventParameters = EventParameterUtils.normalizePreserveOrder(event.getParmCollection()).stream()
            .map(p -> new OnmsEventParameter(this, p))
            .collect(Collectors.toList());
        setPositionsOnParameters(eventParameters);
    }

    public void addEventParameter(OnmsEventParameter parameter) {
        if (eventParameters == null) {
            eventParameters = new ArrayList<>();
        }
        if (eventParameters.contains(parameter)) {
            eventParameters.remove(parameter);
        }
        eventParameters.add(parameter);
        setPositionsOnParameters(eventParameters);
    }

    /**
     * We need this method to preserve the order in the m_eventParameters when saved and retrieved from the database.
     * There might be a more elegant solution via JPA but none seems to work in our context, see also:
     * https://issues.opennms.org/browse/NMS-9827
     */
    private void setPositionsOnParameters(List<OnmsEventParameter> parameters) {
        if (parameters != null) {
            // give each parameter a distinct position
            for (int i = 0; i < parameters.size(); i++) {
                parameters.get(i).setPosition(i);
            }
        }
    }

    /**
     * <p>getEventCreateTime</p>
     *
     * @return a {@link Date} object.
     */
    public Date getEventCreateTime() {
        return eventCreateTime;
    }

    /**
     * <p>setEventCreateTime</p>
     *
     * @param eventcreatetime a {@link Date} object.
     */
    public void setEventCreateTime(Date eventcreatetime) {
        eventCreateTime = eventcreatetime;
    }

    /**
     * <p>getEventDescr</p>
     *
     * @return a {@link String} object.
     */
    public String getEventDescr() {
        return eventDescr;
    }

    /**
     * <p>setEventDescr</p>
     *
     * @param eventdescr a {@link String} object.
     */
    public void setEventDescr(String eventdescr) {
        eventDescr = eventdescr;
    }

    /**
     * <p>getEventLogGroup</p>
     *
     * @return a {@link String} object.
     */
    public String getEventLogGroup() {
        return eventLogGroup;
    }

    /**
     * <p>setEventLogGroup</p>
     *
     * @param eventloggroup a {@link String} object.
     */
    public void setEventLogGroup(String eventloggroup) {
        eventLogGroup = eventloggroup;
    }

    /**
     * <p>getEventLogMsg</p>
     *
     * @return a {@link String} object.
     */
    public String getEventLogMsg() {
        return eventLogMsg;
    }

    /**
     * <p>setEventLogMsg</p>
     *
     * @param eventlogmsg a {@link String} object.
     */
    public void setEventLogMsg(String eventlogmsg) {
        eventLogMsg = eventlogmsg;
    }

    /**
     * <p>getEventSeverity</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getEventSeverity() {
        return eventSeverity;
    }

    /**
     * <p>setEventSeverity</p>
     *
     * @param severity a {@link Integer} object.
     */
    public void setEventSeverity(Integer severity) {
        eventSeverity = severity;
    }

    /**
     * <p>getSeverityLabel</p>
     *
     * @return a {@link String} object.
     */
    public String getSeverityLabel() {
        return OnmsSeverity.get(eventSeverity).name();
    }

    /**
     * <p>setSeverityLabel</p>
     *
     * @param label a {@link String} object.
     */
    public void setSeverityLabel(String label) {
        eventSeverity = OnmsSeverity.get(label).getId();
    }


    /**
     * <p>getEventPathOutage</p>
     *
     * @return a {@link String} object.
     */
    public String getEventPathOutage() {
        return eventPathOutage;
    }

    /**
     * <p>setEventPathOutage</p>
     *
     * @param eventpathoutage a {@link String} object.
     */
    public void setEventPathOutage(String eventpathoutage) {
        eventPathOutage = eventpathoutage;
    }

    /**
     * <p>getEventCorrelation</p>
     *
     * @return a {@link String} object.
     */
    public String getEventCorrelation() {
        return eventCorrelation;
    }

    /**
     * <p>setEventCorrelation</p>
     *
     * @param eventcorrelation a {@link String} object.
     */
    public void setEventCorrelation(String eventcorrelation) {
        eventCorrelation = eventcorrelation;
    }

    /**
     * <p>getEventSuppressedCount</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getEventSuppressedCount() {
        return eventSuppressedCount;
    }

    /**
     * <p>setEventSuppressedCount</p>
     *
     * @param eventsuppressedcount a {@link Integer} object.
     */
    public void setEventSuppressedCount(Integer eventsuppressedcount) {
        eventSuppressedCount = eventsuppressedcount;
    }

    /**
     * <p>getEventOperInstruct</p>
     *
     * @return a {@link String} object.
     */
    public String getEventOperInstruct() {
        return eventOperInstruct;
    }

    /**
     * <p>setEventOperInstruct</p>
     *
     * @param eventoperinstruct a {@link String} object.
     */
    public void setEventOperInstruct(String eventoperinstruct) {
        eventOperInstruct = eventoperinstruct;
    }

    /**
     * <p>getEventAutoAction</p>
     *
     * @return a {@link String} object.
     */
    public String getEventAutoAction() {
        return eventAutoAction;
    }

    /**
     * <p>setEventAutoAction</p>
     *
     * @param eventautoaction a {@link String} object.
     */
    public void setEventAutoAction(String eventautoaction) {
        eventAutoAction = eventautoaction;
    }

    /**
     * <p>getEventOperAction</p>
     *
     * @return a {@link String} object.
     */
    public String getEventOperAction() {
        return eventOperAction;
    }

    /**
     * <p>setEventOperAction</p>
     *
     * @param eventoperaction a {@link String} object.
     */
    public void setEventOperAction(String eventoperaction) {
        eventOperAction = eventoperaction;
    }

    /**
     * <p>getEventOperActionMenuText</p>
     *
     * @return a {@link String} object.
     */
    public String getEventOperActionMenuText() {
        return eventOperActionMenuText;
    }

    /**
     * <p>setEventOperActionMenuText</p>
     *
     * @param eventOperActionMenuText a {@link String} object.
     */
    public void setEventOperActionMenuText(String eventOperActionMenuText) {
        this.eventOperActionMenuText = eventOperActionMenuText;
    }

    /**
     * <p>getEventNotification</p>
     *
     * @return a {@link String} object.
     */
    public String getEventNotification() {
        return eventNotification;
    }

    /**
     * <p>setEventNotification</p>
     *
     * @param eventnotification a {@link String} object.
     */
    public void setEventNotification(String eventnotification) {
        eventNotification = eventnotification;
    }

    /**
     * <p>getEventTTicket</p>
     *
     * @return a {@link String} object.
     */
    public String getEventTTicket() {
        return eventTTicket;
    }

    /**
     * <p>setEventTTicket</p>
     *
     * @param eventtticket a {@link String} object.
     */
    public void setEventTTicket(String eventtticket) {
        eventTTicket = eventtticket;
    }

    /**
     * <p>getEventTTicketState</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getEventTTicketState() {
        return eventTTicketState;
    }

    /**
     * <p>setEventTTicketState</p>
     *
     * @param eventtticketstate a {@link Integer} object.
     */
    public void setEventTTicketState(Integer eventtticketstate) {
        eventTTicketState = eventtticketstate;
    }

    /**
     * <p>getEventForward</p>
     *
     * @return a {@link String} object.
     */
    public String getEventForward() {
        return eventForward;
    }

    /**
     * <p>setEventForward</p>
     *
     * @param eventforward a {@link String} object.
     */
    public void setEventForward(String eventforward) {
        eventForward = eventforward;
    }

    /**
     * <p>getEventMouseOverText</p>
     *
     * @return a {@link String} object.
     */
    public String getEventMouseOverText() {
        return eventMouseOverText;
    }

    /**
     * <p>setEventMouseOverText</p>
     *
     * @param eventmouseovertext a {@link String} object.
     */
    public void setEventMouseOverText(String eventmouseovertext) {
        eventMouseOverText = eventmouseovertext;
    }

    /**
     * TODO: Make this an Enum
     *
     * @return a {@link String} object.
     */
    public String getEventLog() {
        return eventLog;
    }

    /**
     * <p>setEventLog</p>
     *
     * @param eventlog a {@link String} object.
     */
    public void setEventLog(String eventlog) {
        eventLog = eventlog;
    }

    /**
     * TODO: make this an Enum
     *
     * @return a {@link String} object.
     */
    public String getEventDisplay() {
        return eventDisplay;
    }

    /**
     * <p>setEventDisplay</p>
     *
     * @param eventdisplay a {@link String} object.
     */
    public void setEventDisplay(String eventdisplay) {
        eventDisplay = eventdisplay;
    }

    /**
     * <p>getEventAckUser</p>
     *
     * @return a {@link String} object.
     */
    public String getEventAckUser() {
        return eventAckUser;
    }

    /**
     * <p>setEventAckUser</p>
     *
     * @param eventackuser a {@link String} object.
     */
    public void setEventAckUser(String eventackuser) {
        eventAckUser = eventackuser;
    }

    /**
     * <p>getEventAckTime</p>
     *
     * @return a {@link Date} object.
     */
    public Date getEventAckTime() {
        return eventAckTime;
    }

    /**
     * <p>setEventAckTime</p>
     *
     * @param eventacktime a {@link Date} object.
     */
    public void setEventAckTime(Date eventacktime) {
        eventAckTime = eventacktime;
    }

    /**
     * <p>getNode</p>
     *
     * @return a {@link OnmsNode} object.
     */
    public OnmsNode getNode() {
        return node;
    }

    public Integer getNodeId() {
        return node != null ? node.getId() : null;
    }

    public String getNodeLabel() {
        if (node == null) return null;
        return node.getLabel();
    }

    /**
     * <p>setNode</p>
     *
     * @param node a {@link OnmsNode} object.
     */
    public void setNode(OnmsNode node) {
        this.node = node;
    }

    /**
     * <p>getNotifications</p>
     *
     * @return a {@link Set} object.
     */
    public Set<OnmsNotification> getNotifications() {
        return notifications;
    }

    /**
     * <p>setNotifications</p>
     *
     * @param notifications a {@link Set} object.
     */
    public void setNotifications(Set<OnmsNotification> notifications) {
        this.notifications = notifications;
    }

    /**
     * <p>getAssociatedServiceRegainedOutages</p>
     *
     * @return a {@link Set} object.
     */
    public Set<OnmsOutage> getAssociatedServiceRegainedOutages() {
        return associatedServiceRegainedOutages;
    }

    /**
     * <p>setAssociatedServiceRegainedOutages</p>
     *
     * @param outagesBySvcregainedeventid a {@link Set} object.
     */
    public void setAssociatedServiceRegainedOutages(Set<OnmsOutage> outagesBySvcregainedeventid) {
        associatedServiceRegainedOutages = outagesBySvcregainedeventid;
    }

    /**
     * <p>getAssociatedServiceLostOutages</p>
     *
     * @return a {@link Set} object.
     */
    public Set<OnmsOutage> getAssociatedServiceLostOutages() {
        return associatedServiceLostOutages;
    }

    /**
     * <p>setAssociatedServiceLostOutages</p>
     *
     * @param outagesBySvclosteventid a {@link Set} object.
     */
    public void setAssociatedServiceLostOutages(Set<OnmsOutage> outagesBySvclosteventid) {
        associatedServiceLostOutages = outagesBySvclosteventid;
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link String} object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("eventid", getId())
            .add("eventuei", getEventUei())
            .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(EntityVisitor visitor) {
        throw new RuntimeException("visitor method not implemented");
    }

    /**
     * <p>getIfIndex</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getIfIndex() {
        return ifIndex;
    }

    /**
     * <p>setIfIndex</p>
     *
     * @param ifIndex a {@link Integer} object.
     */
    public void setIfIndex(Integer ifIndex) {
        this.ifIndex = ifIndex;
    }
}
