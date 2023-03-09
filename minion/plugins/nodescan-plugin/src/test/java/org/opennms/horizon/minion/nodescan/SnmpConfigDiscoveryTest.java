package org.opennms.horizon.minion.nodescan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpConfiguration;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.snmp4j.Snmp4JValue;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.snmp4j.smi.Integer32;

class SnmpConfigDiscoveryTest {
    @Mock
    private SnmpHelper snmpHelper;

    private SnmpConfigDiscovery snmpConfigDiscovery;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        snmpConfigDiscovery = new SnmpConfigDiscovery(snmpHelper);
    }

    @Test
    void detectSNMP() {
        SnmpAgentConfig config1 = new SnmpAgentConfig(InetAddressUtils.getInetAddress("127.0.0.1"), SnmpConfiguration.DEFAULTS);
        SnmpAgentConfig config2 = new SnmpAgentConfig(InetAddressUtils.getInetAddress("192.168.1.1"), SnmpConfiguration.DEFAULTS);
        List<SnmpAgentConfig> configs = Arrays.asList(config1, config2);

        SnmpValue[] snmpValues1 = new SnmpValue[]{new Snmp4JValue(new Integer32(1))};
        SnmpValue[] snmpValues2 = new SnmpValue[0];

        when(snmpHelper.getAsync(config1, new SnmpObjId[]{SnmpObjId.get(SnmpHelper.SYSTEM_OID)}))
            .thenReturn(CompletableFuture.completedFuture(snmpValues1));
        when(snmpHelper.getAsync(config2, new SnmpObjId[]{SnmpObjId.get(SnmpHelper.SYSTEM_OID)}))
            .thenReturn(CompletableFuture.completedFuture(snmpValues2));

        List<SnmpAgentConfig> detectedConfigs = snmpConfigDiscovery.getDiscoveredConfig(configs);

        assertEquals(1, detectedConfigs.size());
        assertEquals(config1, detectedConfigs.get(0));

        verify(snmpHelper, times(2)).getAsync(any(), any());
    }
}

