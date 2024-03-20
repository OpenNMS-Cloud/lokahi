package org.opennms.horizon.shared.snmp.syslog;

import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyslogIdentity {

    private static final transient Logger LOG = LoggerFactory.getLogger(SyslogIdentity.class);

    private int m_generic;
    private int m_specific;
    private String m_enterpriseId;
    private String syslogOID;

    /**
     * The standard traps list
     */
    private static final List<SnmpObjId> GENERIC_TRAPS;

    private static final Map<Integer, SnmpObjId> GENERIC_TRAPS_BY_GENERIC_ID;

    /**
     * The dot separator in an OID
     */
    private static final char DOT_CHAR = '.';

    /**
     * The SNMP trap enterprise OID, which if present in a V2 trap is the last
     * varbind.
     *
     * ref - book 'SNMP, SNMPv2, SNMPv3..' by William Stallings, third edition,
     * section 13.1.3
     */
    private static final String SNMP_TRAP_ENTERPRISE_ID = ".1.3.6.1.6.3.1.1.4.3.0";

    /**
     * The snmpTraps value to be used in case a standard trap comes in without
     * the SNMP_TRAP_ENTERPRISE_ID as the last varbind.
     */
    private static final String SNMP_TRAPS = ".1.3.6.1.6.3.1.1.5";

    /**
     * Create the standard traps list - used in v2 processing
     */
    static {
        GENERIC_TRAPS = new ArrayList<>();
        GENERIC_TRAPS.add(new SnmpObjId("1.3.6.1.6.3.1.1.5.1")); // coldStart
        GENERIC_TRAPS.add(new SnmpObjId("1.3.6.1.6.3.1.1.5.2")); // warmStart
        GENERIC_TRAPS.add(new SnmpObjId("1.3.6.1.6.3.1.1.5.3")); // linkDown
        GENERIC_TRAPS.add(new SnmpObjId("1.3.6.1.6.3.1.1.5.4")); // linkUp
        GENERIC_TRAPS.add(new SnmpObjId("1.3.6.1.6.3.1.1.5.5")); // authenticationFailure
        GENERIC_TRAPS.add(new SnmpObjId("1.3.6.1.6.3.1.1.5.6")); // egpNeighborLoss
    }

    static {
        GENERIC_TRAPS_BY_GENERIC_ID = new HashMap<>();
        GENERIC_TRAPS_BY_GENERIC_ID.put(0, new SnmpObjId("1.3.6.1.6.3.1.1.5.1")); // coldStart
        GENERIC_TRAPS_BY_GENERIC_ID.put(1, new SnmpObjId("1.3.6.1.6.3.1.1.5.2")); // warmStart
        GENERIC_TRAPS_BY_GENERIC_ID.put(2, new SnmpObjId("1.3.6.1.6.3.1.1.5.3")); // linkDown
        GENERIC_TRAPS_BY_GENERIC_ID.put(3, new SnmpObjId("1.3.6.1.6.3.1.1.5.4")); // linkUp
        GENERIC_TRAPS_BY_GENERIC_ID.put(4, new SnmpObjId("1.3.6.1.6.3.1.1.5.5")); // authenticationFailure
        GENERIC_TRAPS_BY_GENERIC_ID.put(5, new SnmpObjId("1.3.6.1.6.3.1.1.5.6")); // egpNeighborLoss
    }

    public SyslogIdentity(SnmpObjId snmpTrapOid, SnmpObjId lastVarBindOid, SnmpValue lastVarBindValue) {
        String snmpTrapOidValue = snmpTrapOid.toString();

        LOG.debug("snmpTrapOID: {}", snmpTrapOidValue);

        // get the last subid
        int lastIndex = snmpTrapOidValue.lastIndexOf(this.syslogOID);
        String lastSubIdStr = snmpTrapOidValue.substring(lastIndex + 1);
        int lastSubId = -1;
        try {
            lastSubId = Integer.parseInt(lastSubIdStr);
        } catch (NumberFormatException nfe) {
            lastSubId = -1;
        }
        // Check if standard trap
        if (SyslogIdentity.GENERIC_TRAPS.contains(snmpTrapOid)) {
            // set generic
            setGeneric(lastSubId - 1);

            // set specific to zero
            setSpecific(0);

            // if present, the 'snmpTrapEnterprise' OID occurs as
            // the last OID
            // Check the last varbind to see if it is the enterprise ID
            String varBindName = lastVarBindOid.toString();
            if (varBindName.equals(SyslogIdentity.SNMP_TRAP_ENTERPRISE_ID)) {
                // if present, set the value of the varbind as the
                // enterprise id
                setEnterpriseId(lastVarBindValue.toString());
            } else {
                // if not present, set the value of the varbind as the
                // snmpTraps value defined as in RFC 1907
                setEnterpriseId(SyslogIdentity.SNMP_TRAPS + "." + snmpTrapOidValue.charAt(snmpTrapOidValue.length() - 1));
            }
            setSyslogOID(getEnterpriseId());
        } else // not standard trap
        {
            // set generic to 6
            setGeneric(6);

            setSpecific(lastSubId);

            // get the next to last subid
            int nextToLastIndex = snmpTrapOidValue.lastIndexOf(SyslogIdentity.DOT_CHAR, lastIndex - 1);
            // check if value is zero
            String nextToLastSubIdStr = snmpTrapOidValue.substring(nextToLastIndex + 1, lastIndex);
            if (nextToLastSubIdStr.equals("0")) {
                // set enterprise value to trap oid minus the
                // the last two subids
                setEnterpriseId(snmpTrapOidValue.substring(0, nextToLastIndex));
                // Parse full trap oid with sub-id
                setSyslogOID(snmpTrapOidValue);
            } else {
                setEnterpriseId(snmpTrapOidValue.substring(0, lastIndex));
                setSyslogOID(snmpTrapOidValue);
            }
        }
    }

    public SyslogIdentity(SnmpObjId entId, int generic, int specific) {
        m_enterpriseId = entId.toString();
        m_generic = generic;
        m_specific = specific;
        // SNMP V1
        if (m_generic == 6) {
            // Concatenate sub-id and specific with enterpriseId.
            String trapOID = m_enterpriseId + "." + 0 + "." + m_specific;
            setSyslogOID(trapOID);
        } else if (GENERIC_TRAPS_BY_GENERIC_ID.containsKey(m_generic)) {
            SnmpObjId trapOID = GENERIC_TRAPS_BY_GENERIC_ID.get(m_generic);
            setSyslogOID(trapOID.toString());
        } else {
            setSyslogOID(getEnterpriseId());
        }
    }

    public int getGeneric() {
        return m_generic;
    }

    private void setGeneric(int generic) {
        m_generic = generic;
    }

    public int getSpecific() {
        return m_specific;
    }

    private void setSpecific(int specific) {
        m_specific = specific;
    }

    public String getEnterpriseId() {
        return m_enterpriseId;
    }

    private void setEnterpriseId(String enterpriseId) {
        m_enterpriseId = enterpriseId;
    }

    public String getSyslogOID() {
        return syslogOID;
    }

    public void setSyslogOID(String syslogOID) {
        this.syslogOID = syslogOID;
    }

    @Override
    public String toString() {
        return "[Generic=" + getGeneric() + ", Specific=" + getSpecific() + ", EnterpriseId=" + getEnterpriseId() + "]";
    }
}
