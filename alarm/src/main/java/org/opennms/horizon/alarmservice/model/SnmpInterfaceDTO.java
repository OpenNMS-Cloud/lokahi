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

package org.opennms.horizon.alarmservice.model;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alarmservice.utils.AlphaNumeric;
import org.opennms.horizon.alarmservice.utils.RrdLabelUtils;
import org.opennms.horizon.alarmservice.utils.SystemProperties;

@Slf4j
@Data
@NoArgsConstructor
public class SnmpInterfaceDTO  implements Serializable {
    private static final long serialVersionUID = 4688655131862954563L;

    private Integer id;
    private String physAddr;
    private Integer ifIndex;
    private String ifDescr;
    private Integer ifType;
    private String ifName;
    private Long ifSpeed;
    private Integer ifAdminStatus;
    private Integer ifOperStatus;
    private String ifAlias;
    private Date lastCapsdPoll;
    private String collect = "N";
    private String poll;
    private Date lastSnmpPoll;
    private Set<IpInterfaceDTO> ipInterfaces = new HashSet<>();
    private Date lastIngressFlow;
    private Date lastEgressFlow;

    public static final int MAX_FLOW_AGE = SystemProperties.getInteger("org.opennms.features.telemetry.maxFlowAgeSeconds", 604800);
    public static final boolean INGRESS_AND_EGRESS_REQUIRED = Boolean.getBoolean("org.opennms.features.telemetry.ingressAndEgressRequired");


    /**
     * <p>Constructor for OnmsSnmpInterface.</p>
     * @param ifIndex a {@link Integer} object.
     */
    public SnmpInterfaceDTO(Integer ifIndex) {
        ifIndex = ifIndex;
    }
    


    /**
     * <p>isCollectionUserSpecified</p>
     *
     * @return a boolean.
     */
    public boolean isCollectionUserSpecified(){
        return collect.startsWith("U");
    }

    /**
     * <p>isCollectionEnabled</p>
     *
     * @return a boolean.
     */
    public boolean isCollectionEnabled() {
        return "C".equals(collect) || "UC".equals(collect);
    }

    /**
     * <p>setCollectionEnabled</p>
     *
     * @param shouldCollect a boolean.
     */
    public void setCollectionEnabled(boolean shouldCollect) {
        setCollectionEnabled(shouldCollect, false);
    }
    
    /**
     * <p>setCollectionEnabled</p>
     *
     * @param shouldCollect a boolean.
     * @param userSpecified a boolean.
     */
    public void setCollectionEnabled(boolean shouldCollect, boolean userSpecified){
       if(userSpecified){
           collect = shouldCollect ? "UC":"UN";
       }else if(!collect.startsWith("U")){
           collect = shouldCollect ? "C" : "N";
       }
    }

    /**
     * <p>isPollEnabled</p>
     *
     * @return a boolean.
     */
    public boolean isPollEnabled() {
        return "P".equals(poll);
    }


    public boolean getHasFlows() {
        if (INGRESS_AND_EGRESS_REQUIRED) {
            return getHasIngressFlows() && getHasEgressFlows();
        } else {
            return getHasIngressFlows() || getHasEgressFlows();
        }
    }

    public boolean getHasIngressFlows() {
        if (lastIngressFlow == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastIngressFlow.getTime()) / 1000 < MAX_FLOW_AGE;
    }

    public boolean getHasEgressFlows() {
        if (lastEgressFlow == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastEgressFlow.getTime()) / 1000 < MAX_FLOW_AGE;
    }


    /**
     * <p>toString</p>
     *
     * @return a {@link String} object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("snmpphysaddr", getPhysAddr())
            .add("snmpifindex", getIfIndex())
            .add("snmpifdescr", getIfDescr())
            .add("snmpiftype", getIfType())
            .add("snmpifname", getIfName())
            .add("snmpifspeed", getIfSpeed())
            .add("snmpifadminstatus", getIfAdminStatus())
            .add("snmpifoperstatus", getIfOperStatus())
            .add("snmpifalias", getIfAlias())
            .add("snmpCollect", getCollect())
            .add("snmpPoll", getPoll())
            .add("lastCapsdPoll", getLastCapsdPoll())
            .add("lastSnmpPoll", getLastSnmpPoll())
            .add("lastIngressFlow", lastIngressFlow)
            .add("lastEgressFlow", lastEgressFlow)
            .toString();
    }


    // @Transient
    // public Set getIpInterfaces() {
    //		
    // Set ifsForSnmpIface = new LinkedHashSet();
    // for (Iterator it = getNode().getIpInterfaces().iterator();
    // it.hasNext();) {
    // OnmsIpInterface iface = (OnmsIpInterface) it.next();
    // if (getIfIndex().equals(iface.getIfIndex()))
    // ifsForSnmpIface.add(iface);
    // }
    // return ifsForSnmpIface;
    // }

    /**
     * <p>computePhysAddrForRRD</p>
     *
     * @return a {@link String} object.
     */
    public String computePhysAddrForRRD() {
        /*
         * In order to assure the uniqueness of the RRD file names we now
         * append the MAC/physical address to the end of label if it is
         * available.
         */
        String physAddrForRRD = null;

        if (getPhysAddr() != null) {
            String parsedPhysAddr = AlphaNumeric.parseAndTrim(getPhysAddr());
            if (parsedPhysAddr.length() == 12) {
                physAddrForRRD = parsedPhysAddr;
            } else {
                    log.debug("physAddrForRRD: physical address len is NOT 12, physAddr={}", parsedPhysAddr);
            }
        }
        log.debug("computed physAddr for {} to be {}", this, physAddrForRRD);
        return physAddrForRRD;
    }

    /**
     * <p>computeNameForRRD</p>
     *
     * @return a {@link String} object.
     */
    public String computeNameForRRD() {
        /*
         * Determine the label for this interface. The label will be used to
         * create the RRD file name which holds SNMP data retreived from the
         * remote agent. If available ifName is used to generate the label
         * since it is guaranteed to be unique. Otherwise ifDescr is used. In
         * either case, all non alpha numeric characters are converted to
         * underscores to ensure that the resuling string will make a decent
         * file name and that RRD won't have any problems using it
         */
	String firstChoice = RrdLabelUtils.PREFER_IFDESCR ? getIfDescr() : getIfName();
	String secondChoice = RrdLabelUtils.PREFER_IFDESCR ? getIfName() : getIfDescr();
        String label = null;
        if (firstChoice != null) {
            label = RrdLabelUtils.DONT_SANITIZE_IFNAME ? firstChoice : AlphaNumeric.parseAndReplace(firstChoice, '_');
        } else if (secondChoice != null) {
            label = RrdLabelUtils.DONT_SANITIZE_IFNAME ? secondChoice : AlphaNumeric.parseAndReplace(secondChoice, '_');
        } else {
            // TODO: Use IfLabel.NO_IFLABEL instead of "no_ifLabel"
            log.info("Interface ({}) has no ifName and no ifDescr...setting to label to 'no_ifLabel'.", this);
            label = "no_ifLabel";
        }
        return label;
    }

    /**
     * <p>computeLabelForRRD</p>
     *
     * @return a {@link String} object.
     */
    public String computeLabelForRRD() {
        return RrdLabelUtils.computeLabelForRRD(getIfName(), getIfDescr(), getPhysAddr());
    }

    /**
     * <p>addIpInterface</p>
     *
     * @param iface a {@link IpInterfaceDTO} object.
     */
    public void addIpInterface(IpInterfaceDTO iface) {
        iface.setSnmpInterface(this);
        ipInterfaces.add(iface);
    }
}
