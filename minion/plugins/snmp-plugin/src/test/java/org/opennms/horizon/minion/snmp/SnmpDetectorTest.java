package org.opennms.horizon.minion.snmp;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.taskset.contract.MonitorType;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SnmpDetectorTest {

    private SnmpDetector target;

    private SnmpHelper mockSnmpHelper;

    private SnmpDetectorRequest testRequest;

    private Any testConfig;

    @Before
    public void setUp() {
        mockSnmpHelper = Mockito.mock(SnmpHelper.class);

        testRequest =
            SnmpDetectorRequest.newBuilder()
                .setHost("127.0.0.1")
                .build();

        testConfig = Any.pack(testRequest);

        target = new SnmpDetector(mockSnmpHelper);
    }

    @Test
    public void testDetect() throws Exception {
        SnmpValue[] snmpValues = {new TestSnmpValue()};
        CompletableFuture<SnmpValue[]> future = CompletableFuture.completedFuture(snmpValues);

        Mockito.when(mockSnmpHelper.getAsync(Mockito.any(SnmpAgentConfig.class), Mockito.any(SnmpObjId[].class))).thenReturn(future);

        CompletableFuture<ServiceDetectorResponse> response = target.detect(testConfig, 1);

        ServiceDetectorResponse serviceDetectorResponse = response.get();

        assertTrue(serviceDetectorResponse.isServiceDetected());
        assertEquals(MonitorType.SNMP, serviceDetectorResponse.getMonitorType());
        assertEquals(testRequest.getHost(), serviceDetectorResponse.getIpAddress());
        assertNull(serviceDetectorResponse.getReason());
    }

    @Test
    public void testDetectSnmpGetThrowsException() throws Exception {
        RuntimeException exception = new RuntimeException("Failed to call snmp get");

        Mockito.when(mockSnmpHelper.getAsync(Mockito.any(SnmpAgentConfig.class), Mockito.any(SnmpObjId[].class))).thenThrow(exception);

        CompletableFuture<ServiceDetectorResponse> response = target.detect(testConfig,  1);

        ServiceDetectorResponse serviceDetectorResponse = response.get();

        assertFalse(serviceDetectorResponse.isServiceDetected());
        assertEquals(MonitorType.SNMP, serviceDetectorResponse.getMonitorType());
        assertEquals(testRequest.getHost(), serviceDetectorResponse.getIpAddress());
        assertEquals(exception.getMessage(), serviceDetectorResponse.getReason());
    }

    private static class TestSnmpValue implements SnmpValue {

        @Override
        public boolean isEndOfMib() {
            return false;
        }

        @Override
        public boolean isError() {
            return false;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isDisplayable() {
            return false;
        }

        @Override
        public boolean isNumeric() {
            return false;
        }

        @Override
        public int toInt() {
            return 0;
        }

        @Override
        public String toDisplayString() {
            return null;
        }

        @Override
        public InetAddress toInetAddress() {
            return null;
        }

        @Override
        public long toLong() {
            return 0;
        }

        @Override
        public BigInteger toBigInteger() {
            return null;
        }

        @Override
        public String toHexString() {
            return null;
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            return new byte[0];
        }

        @Override
        public SnmpObjId toSnmpObjId() {
            return null;
        }
    }
}
