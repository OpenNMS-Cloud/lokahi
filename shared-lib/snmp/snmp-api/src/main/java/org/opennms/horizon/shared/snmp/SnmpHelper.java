package org.opennms.horizon.shared.snmp;

import org.opennms.horizon.shared.snmp.traps.TrapNotificationListener;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SnmpHelper {

    /**
     * Object identifier used to retrieve device system information
     */
    String SYSTEM_OID = ".1.3.6.1.2.1.1";

    /**
     * Object identifier used to retrieve interface count. This is the MIB-II
     * interfaces.ifNumber value.
     */
    String INTERFACES_IFNUMBER = ".1.3.6.1.2.1.2.1";

    /**
     * Object identifier used to retrieve system uptime. This is the MIB-II
     * system.sysUpTime value.
     */
    String NODE_SYSUPTIME = ".1.3.6.1.2.1.1.3";

    String IFINDEX_OID = ".1.3.6.1.2.1.4.20.1.2";

    SnmpWalker createWalker(SnmpAgentConfig agentConfig, String name, CollectionTracker... trackers);

    SnmpWalker createWalker(SnmpAgentConfig agentConfig, String name, CollectionTracker tracker);

    SnmpValue get(SnmpAgentConfig agentConfig, SnmpObjId oid);

    SnmpValue[] get(SnmpAgentConfig agentConfig, SnmpObjId[] oids);

    CompletableFuture<SnmpValue[]> getAsync(SnmpAgentConfig agentConfig, SnmpObjId[] oids);

    SnmpValue getNext(SnmpAgentConfig agentConfig, SnmpObjId oid);

    SnmpValue[] getNext(SnmpAgentConfig agentConfig, SnmpObjId[] oids);

    SnmpValue[] getBulk(SnmpAgentConfig agentConfig, SnmpObjId[] oids);

    SnmpValue set(SnmpAgentConfig agentConfig, SnmpObjId oid, SnmpValue value);

    SnmpValue[] set(SnmpAgentConfig agentConfig, SnmpObjId[] oids, SnmpValue[] values);

    List<SnmpValue> getColumns(SnmpAgentConfig agentConfig, String name, SnmpObjId oid)  throws InterruptedException;

    Map<SnmpInstId, SnmpValue> getOidValues(SnmpAgentConfig agentConfig, String name, SnmpObjId oid)
    throws InterruptedException;

    void registerForTraps(TrapNotificationListener listener, InetAddress address, int snmpTrapPort, List<SnmpV3User> snmpUsers) throws IOException;

    void registerForTraps(TrapNotificationListener listener, InetAddress address, int snmpTrapPort) throws IOException;

    void unregisterForTraps(TrapNotificationListener listener) throws IOException;

    SnmpValueFactory getValueFactory();

    SnmpV1TrapBuilder getV1TrapBuilder();

    SnmpTrapBuilder getV2TrapBuilder();

    SnmpV3TrapBuilder getV3TrapBuilder();

    SnmpV2TrapBuilder getV2InformBuilder();

    SnmpV3TrapBuilder getV3InformBuilder();

    String getLocalEngineID();

    String getHexString(byte[] raw);

    Long getProtoCounter63Value(SnmpValue value);

    Long getProtoCounter63Value(byte[] valBytes);

    SnmpStrategy getStrategy();
}
