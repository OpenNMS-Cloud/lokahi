/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2022 The OpenNMS Group, Inc.
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

import static org.opennms.horizon.shared.utils.InetAddressUtils.addr;
import static org.opennms.horizon.shared.utils.InetAddressUtils.str;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;

import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Abstract EventUtils class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public abstract class EventUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(EventUtils.class);

    
    /**
     * <p>createNodeAddedEvent</p>
     *
     * @param source a {@link String} object.
     * @param nodeId a int.
     * @param nodeLabel a {@link String} object.
     * @param labelSource a {@link String} object.
     * @param monitorKey a {@link String} object. (optional)
     * @return a {@link Event} object.
     */
    public static Event createNodeAddedEvent(String source, int nodeId, String nodeLabel, OnmsNode.NodeLabelSource labelSource, String monitorKey) {
        debug("CreateNodeAddedEvent: nodedId: %d", nodeId);
        
        EventBuilder bldr = new EventBuilder(EventConstants.NODE_ADDED_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.addParam(EventConstants.PARM_NODE_LABEL, WebSecurityUtils.sanitizeString(nodeLabel));
        if (labelSource != null) {
            bldr.addParam(EventConstants.PARM_NODE_LABEL_SOURCE, labelSource.toString());
        }
        if (monitorKey != null) {
            bldr.addParam(EventConstants.PARM_MONITOR_KEY, monitorKey);
        }
        return bldr.getEvent();
    }

    /**
     * <p>createNodeGainedInterfaceEvent</p>
     *
     * @param source a {@link String} object.
     * @param nodeId a int.
     * @param ifaddr a {@link InetAddress} object.
     * @return a {@link Event} object.
     */
    public static Event createNodeGainedInterfaceEvent(String source, int nodeId, InetAddress ifaddr) {
        
        debug("createNodeGainedInterfaceEvent:  %d / %s", nodeId, str(ifaddr));
        
        EventBuilder bldr = new EventBuilder(EventConstants.NODE_GAINED_INTERFACE_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.setInterface(ifaddr);
        bldr.addParam(EventConstants.PARM_IP_HOSTNAME, ifaddr.getHostName());

        return bldr.getEvent();
    }
    
    /**
     * <p>createNodeGainedServiceEvent</p>
     *
     * @param source a {@link String} object.
     * @param nodeId a int.
     * @param ifaddr a {@link InetAddress} object.
     * @param service a {@link String} object.
     * @param nodeLabel a {@link String} object.
     * @param labelSource a {@link String} object.
     * @param sysName a {@link String} object.
     * @param sysDescr a {@link String} object.
     * @return a {@link Event} object.
     */
    public static Event createNodeGainedServiceEvent(String source, int nodeId, InetAddress ifaddr, String service, String nodeLabel, OnmsNode.NodeLabelSource labelSource, String sysName, String sysDescr) {
        
        debug("createAndSendNodeGainedServiceEvent:  nodeId/interface/service  %d/%s/%s", nodeId, str(ifaddr), service);

        EventBuilder bldr = new EventBuilder(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.setInterface(ifaddr);
        bldr.setService(service);
        bldr.setParam(EventConstants.PARM_IP_HOSTNAME, ifaddr.getHostName());
        bldr.setParam(EventConstants.PARM_NODE_LABEL, nodeLabel);
        if (labelSource != null) {
            bldr.setParam(EventConstants.PARM_NODE_LABEL_SOURCE, labelSource.toString());
        }

        // Add sysName if available
        if (sysName != null) {
            bldr.setParam(EventConstants.PARM_NODE_SYSNAME, sysName);
        }

        // Add sysDescr if available
        if (sysDescr != null) {
            bldr.setParam(EventConstants.PARM_NODE_SYSDESCRIPTION, sysDescr);
        }

        return bldr.getEvent();
    }

    /**
     * This method is responsible for generating a nodeDeleted event and sending
     * it to eventd..
     *
     * @param source
     *            A string representing the source of the event
     * @param nodeId
     *            Nodeid of the node got deleted.
     * @param hostName
     *            the Host server name.
     * @param nodeLabel
     *            the node label of the deleted node.
     * @return a {@link Event} object.
     */
    public static Event createNodeDeletedEvent(final String source, final int nodeId, final String hostName, final String nodeLabel, final OnmsMonitoringLocation nodeLocation, final String nodeForeignId, final String nodeForeignSource, final OnmsIpInterface nodePrimaryInterface) {
        
        debug("createNodeDeletedEvent for nodeid:  %d", nodeId);

        final EventBuilder bldr = new EventBuilder(EventConstants.NODE_DELETED_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.setHost(hostName);

        if (nodeLabel != null) {
            bldr.addParam(EventConstants.PARM_NODE_LABEL, nodeLabel);
        }

        if (nodeLocation != null) {
            bldr.addParam(EventConstants.PARM_LOCATION, nodeLocation.getLocationName());
        }

        if (nodeForeignId != null) {
            bldr.addParam(EventConstants.PARM_FOREIGN_ID, nodeForeignId);
        }

        if (nodeForeignSource != null) {
            bldr.addParam(EventConstants.PARM_FOREIGN_SOURCE, nodeForeignSource);
        }

        if (nodePrimaryInterface != null && nodePrimaryInterface.getIpAddress() != null) {
            bldr.addParam(EventConstants.PARM_INTERFACE, InetAddressUtils.str(nodePrimaryInterface.getIpAddress()));
        }

        return bldr.getEvent();
    }

    /**
     * Construct an interfaceDeleted event for an interface.
     *
     * @param source
     *            the source of the event
     * @param nodeId
     *            the nodeId of the node the interface resides in
     * @param addr
     *            the ipAdddr of the event
     * @param ipInterfaceId
     *            the  id of IpInterface
     * @return an Event represent an interfaceDeleted event for the given
     *         interface
     */
    public static Event createInterfaceDeletedEvent(String source, int nodeId, InetAddress addr, Integer ipInterfaceId) {
        debug("createInterfaceDeletedEvent for nodeid/ipaddr:  %d/%s", nodeId, str(addr));

        EventBuilder bldr = new EventBuilder(EventConstants.INTERFACE_DELETED_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.setInterface(addr);
        if(ipInterfaceId != null) {
            bldr.addParam(EventConstants.PARM_IPINTERFACE_ID, ipInterfaceId);
        }
        return bldr.getEvent();
    }

    /**
     * Constructs a serviceDeleted Event for the nodeId, ipAddr, serviceName
     * triple
     *
     * @param source
     *            the source of the event
     * @param nodeId
     *            the nodeId that the service resides on
     * @param addr
     *            the interface that the service resides on
     * @param service
     *            the name of the service that was deleted
     * @return an Event that represents the serviceDeleted event for the give
     *         triple
     */
    public static Event createServiceDeletedEvent(String source, int nodeId, InetAddress addr, String service) {
        debug("createServiceDeletedEvent for nodeid/ipaddr/service:  %d/%s", nodeId, str(addr), service);

        EventBuilder bldr = new EventBuilder(EventConstants.SERVICE_DELETED_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.setInterface(addr);
        bldr.setService(service);

        return bldr.getEvent();
    }

    /**
     * Constructs a applicationDeleted Event for a given application id and name
     * @param source
     *              the source of the event
     * @param applicationId
     *              the id of the deleted application
     * @param applicationName
     *              the name of the deleted application
     * @return an Event that represents the applicationDeleted event for the given id and name
     */
    public static Event createApplicationDeletedEvent(String source, int applicationId, String applicationName) {
        debug("createApplicationDeletedEvent for nodeid:  %d", applicationId);

        final EventBuilder bldr = new EventBuilder(EventConstants.APPLICATION_DELETED_EVENT_UEI, source);
        bldr.addParam(EventConstants.PARM_APPLICATION_ID, applicationId);
        bldr.addParam(EventConstants.PARM_APPLICATION_NAME, applicationName);

        return bldr.getEvent();
    }


    /**
     * Retrieve the value associated with an event parameter and parse it to a
     * long. If the value can not be found, return a default value.
     *
     * @param e
     *            the Event to retrieve the parameter from
     * @param parmName
     *            the name of the parameter to retrieve
     * @param defaultValue
     *            the value to return if the paramter can not be retrieved or
     *            parsed
     * @return the value of the parameter as a long
     */
    public static long getLongParm(Event e, String parmName, long defaultValue) {
        String longVal = EventUtils.getParm(e, parmName);
    
        if (longVal == null)
            return defaultValue;
    
        try {
            return Long.parseLong(longVal);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Retrieve the value associated with an event parameter and parse it to an
     * int. If the value can not be found, return a default value.
     *
     * @param e
     *            the Event to retrieve the parameter from
     * @param parmName
     *            the name of the parameter to retrieve
     * @param defaultValue
     *            the value to return if the paramter can not be retrieved or
     *            parsed
     * @return the value of the parameter as a long
     */
    public static int getIntParm(Event e, String parmName, int defaultValue) {
        String intVal = EventUtils.getParm(e, parmName);
    
        if (intVal == null)
            return defaultValue;
    
        try {
            return Integer.parseInt(intVal);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Retrieve a parameter from and event, returning defaultValue of the
     * parameter is not set.
     *
     * @param e
     *            The Event to retrieve the parameter from
     * @param parmName
     *            the name of the parameter to retrieve
     * @param defaultValue
     *            the default value to return if the parameter is not set
     * @return the value of the parameter, or defalutValue if the parameter is
     *         not set
     */
    public static String getParm(Event e, String parmName, String defaultValue) {
        if (e.getParmCollection().size() < 1)
            return defaultValue;
    
        for (Parm parm : e.getParmCollection()) {
            if (parmName.equals(parm.getParmName())) {
                if (parm.getValue() != null && parm.getValue().getContent() != null) {
                    return parm.getValue().getContent();
                } else {
                    return defaultValue;
                }
            }
        }
    
        return defaultValue;
    
    }

    /**
     * Return the value of an event parameter of null if it does not exist.
     *
     * @param e
     *            the Event to get the parameter for
     * @param parmName
     *            the name of the parameter to retrieve
     * @return the value of the parameter, or null of the parameter is not set
     */
    public static String getParm(Event e, String parmName) {
        return EventUtils.getParm(e, parmName, null);
    }
    
    private static void debug(String format, Object... args) {
            LOG.debug(String.format(format, args));
    }


    /**
     * <p>createNodeUpdatedEvent</p>
     *
     * @param source a {@link String} object.
     * @param nodeId a {@link Integer} object.
     * @param nodeLabel a {@link String} object.
     * @param labelSource a {@link String} object.
     * @param rescanExisting a {@link String} object.
     * @param monitorKey a {@link String} object. (optional)
     * @return a {@link Event} object.
     */
    public static Event createNodeUpdatedEvent(String source, Integer nodeId, String nodeLabel, OnmsNode.NodeLabelSource labelSource, String rescanExisting, String monitorKey) {
        debug("CreateNodeUpdatedEvent: nodedId: %d", nodeId);
        EventBuilder bldr = new EventBuilder(EventConstants.NODE_UPDATED_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.addParam(EventConstants.PARM_NODE_LABEL, nodeLabel);
        if (labelSource != null) {
            bldr.addParam(EventConstants.PARM_NODE_LABEL_SOURCE, labelSource.toString());
        }
        if (rescanExisting != null) {
            bldr.addParam(EventConstants.PARM_RESCAN_EXISTING, rescanExisting);
        }
        if (monitorKey != null) {
            bldr.addParam(EventConstants.PARM_MONITOR_KEY, monitorKey);
        }
        return bldr.getEvent();
    }
    
    public static Event createNodeRescanEvent(String source, Integer nodeId) {
        debug("CreateNodeUpdatedEvent: nodedId: %d", nodeId);
        EventBuilder bldr = new EventBuilder(EventConstants.RELOAD_IMPORT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.addParam(EventConstants.PARM_RESCAN_EXISTING, Boolean.TRUE.toString());
        return bldr.getEvent();
    }


    /**
     * <p>createNodeLocationChangedEvent</p>
     *
     * @param source a {@link String} object.
     * @param nodeId a {@link Integer} object.
     * @param nodeLabel a {@link String} object.
     * @param prevLocation a {@link String} object.
     * @param currentLocation a {@link String} object.
     * @return a {@link Event} object.
     */
    public static Event createNodeLocationChangedEvent(String source, Integer nodeId, String nodeLabel, String prevLocation, String currentLocation) {
        debug("createNodeLocationChangedEvent: nodedId: %d", nodeId);
        EventBuilder bldr = new EventBuilder(EventConstants.NODE_LOCATION_CHANGED_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.addParam(EventConstants.PARM_NODE_LABEL, nodeLabel);
        bldr.addParam(EventConstants.PARM_NODE_PREV_LOCATION, prevLocation);
        bldr.addParam(EventConstants.PARM_NODE_CURRENT_LOCATION, currentLocation);
        return bldr.getEvent();
    }


    public static Event createNodeCategoryMembershipChangedEvent(final String source, final Integer nodeId, final String nodeLabel, String[] categoriesAdded, String[] categoriesDeleted) {
        EventBuilder bldr = new EventBuilder(EventConstants.NODE_CATEGORY_MEMBERSHIP_CHANGED_EVENT_UEI, source);
        bldr.setNodeid(nodeId);
        bldr.addParam(EventConstants.PARM_NODE_LABEL, nodeLabel);
        if (categoriesAdded != null && categoriesAdded.length > 0) {
            bldr.addParam(EventConstants.PARM_CATEGORIES_ADDED, String.join(",", categoriesAdded));
        }
        if (categoriesDeleted != null && categoriesDeleted.length > 0) {
            bldr.addParam(EventConstants.PARM_CATEGORIES_DELETED, String.join(",", categoriesDeleted));
        }
        return bldr.getEvent();
    }

    public static String toString(Collection<Parm> parms) {
        if (parms.size() == 0) {
            return "{}\n";
        }
        
        final StringBuilder b = new StringBuilder();
        b.append("{\n");
        for (Parm p : parms) {
            b.append("  ");
            b.append(p.getParmName());
            b.append(" = ");
            b.append(toString(p.getValue()));
            b.append("\n");
        }
        b.append(" }");
        return b.toString();
    }
    
    /**
     * <p>toString</p>
     *
     * @param value a {@link Value} object.
     * @return a {@link String} object.
     */
    public static String toString(Value value) {
        return value.getType() + "(" + value.getEncoding() + "): " + value.getContent();
    }

    /**
     * <p>toString</p>
     *
     * @param snmp a {@link Snmp} object.
     * @return a {@link String} object.
     */
    public static String toString(Snmp snmp) {
        StringBuffer b = new StringBuffer("Snmp: ");
    
        if (snmp.getVersion() != null) {
            b.append("Version: " + snmp.getVersion() + "\n");
        }
        
        b.append("TimeStamp: " + new Date(snmp.getTimeStamp()) + "\n");
        
        if (snmp.getCommunity() != null) {
            b.append("Community: " + snmp.getCommunity() + "\n");
        }
    
        b.append("Generic: " + snmp.getGeneric() + "\n");
        b.append("Specific: " + snmp.getSpecific() + "\n");
        
        if (snmp.getId() != null) {
            b.append("Id: " + snmp.getId() + "\n");
        }
        if (snmp.getIdtext() != null) {
            b.append("Idtext: " + snmp.getIdtext() + "\n");
        }
        
        b.append("End Snmp\n");
        return b.toString();
    }


    /**
     * Constructs a deleteInterface event for the given nodeId, ipAddress (or ifIndex) pair.
     *
     * @param source
     *            the source for the event
     * @param nodeId
     *            the nodeId of the node that owns the interface
     * @param ipAddr
     *            the ipAddress of the interface being deleted
     * @param ifIndex
     *            the ifIndex of the interface being deleted
     * @param txNo
     *            the transaction number to use for processing this event
     * @return an Event representing a deleteInterface event for the given
     *         nodeId, ipaddr
     */
    public static Event createDeleteInterfaceEvent(String source, long nodeId, String ipAddr, int ifIndex, long txNo) {
        return createInterfaceEventBuilder(EventConstants.DELETE_INTERFACE_EVENT_UEI, source, nodeId, ipAddr, ifIndex, txNo).getEvent();
    }

    private static EventBuilder createInterfaceEventBuilder(String uei, String source, long nodeId, String ipAddr, int ifIndex, long txNo) {
        EventBuilder bldr = new EventBuilder(uei, source);
        
        if (ipAddr != null && ipAddr.length() != 0) {
            bldr.setInterface(addr(ipAddr));
        }
        
        bldr.setNodeid(nodeId);

        if (ifIndex != -1) {
            bldr.setIfIndex(ifIndex);
        }

        bldr.addParam(EventConstants.PARM_TRANSACTION_NO, txNo);

        return bldr;
    }

    public static Event createDeleteNodeEvent(String source, long nodeId, long txNo) {
        return createNodeEventBuilder(EventConstants.DELETE_NODE_EVENT_UEI, source, nodeId, txNo).getEvent();
    }

    private static EventBuilder createNodeEventBuilder(String uei, String source, long nodeId, long txNo) {
        EventBuilder bldr = new EventBuilder(uei, source);
        
        bldr.setNodeid(nodeId);
        
        if (txNo >= 0) {
            bldr.addParam(EventConstants.PARM_TRANSACTION_NO, txNo);
        }
        return bldr;
    }

    /**
     * Construct a deleteNode event for the given nodeId.
     *
     * @param source
     *            the source for the event
     * @param nodeId
     *            the node to be deleted.
     * @param txNo
     *            the transaction number associated with deleting the node
     * @return an Event object representing a delete node event.
     */
    public static Event createAssetInfoChangedEvent(String source, long nodeId, long txNo) {
        return createNodeEventBuilder(EventConstants.ASSET_INFO_CHANGED_EVENT_UEI, source, nodeId, txNo).getEvent();
    }

    private static EventBuilder createServiceEventBuilder(String uei, String source, long nodeId, String ipAddr, String service, long txNo) {
        EventBuilder bldr = new EventBuilder(uei, source);

        bldr.setNodeid(nodeId);
        bldr.setInterface(addr(ipAddr));
        bldr.setService(service);
        
        bldr.addParam(EventConstants.PARM_TRANSACTION_NO, txNo);

        return bldr;
    }

    /**
     * Constructs a deleteService event for the given nodeId, ipAddress,
     * serviceName triple.
     *
     * @param source
     *            the source for the event
     * @param nodeId
     *            the nodeId of the node that service resides on
     * @param ipAddr
     *            the ipAddress of the interface the service resides on
     * @param service
     *            the service that is being deleted
     * @param txNo
     *            the transaction number to use for processing this event
     * @return an Event representing a deleteInterface event for the given
     *         nodeId, ipaddr
     */
    public static Event createDeleteServiceEvent(String source, long nodeId, String ipAddr, String service, long txNo) {
        
        return createServiceEventBuilder(EventConstants.DELETE_SERVICE_EVENT_UEI, source, nodeId, ipAddr, service, txNo).getEvent();
        
    }
}
