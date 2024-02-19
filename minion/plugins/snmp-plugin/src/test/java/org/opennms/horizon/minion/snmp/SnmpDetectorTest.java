/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.snmp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.Any;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.ServiceResult;
import org.opennms.snmp.contract.SnmpDetectorRequest;

public class SnmpDetectorTest {

    private SnmpDetector target;

    private SnmpHelper mockSnmpHelper;

    private SnmpDetectorRequest testRequest;

    private Any testConfig;

    @Before
    public void setUp() {
        mockSnmpHelper = Mockito.mock(SnmpHelper.class);

        testRequest = SnmpDetectorRequest.newBuilder().setHost("127.0.0.1").build();

        testConfig = Any.pack(testRequest);

        target = new SnmpDetector(mockSnmpHelper);
    }

    @Test
    public void testDetect() throws Exception {
        SnmpValue[] snmpValues = {new TestSnmpValue()};
        CompletableFuture<SnmpValue[]> future = CompletableFuture.completedFuture(snmpValues);

        Mockito.when(mockSnmpHelper.getAsync(Mockito.any(SnmpAgentConfig.class), Mockito.any(SnmpObjId[].class)))
                .thenReturn(future);

        CompletableFuture<ServiceResult> response = target.detect("127.0.0.1", testConfig);

        ServiceResult serviceDetectorResponse = response.get();

        assertTrue(serviceDetectorResponse.getStatus());
        assertEquals(ServiceType.SNMP, serviceDetectorResponse.getService());
        assertEquals(testRequest.getHost(), serviceDetectorResponse.getIpAddress());
    }

    @Test
    public void testDetectSnmpGetThrowsException() throws Exception {
        RuntimeException exception = new RuntimeException("Failed to call snmp get");

        Mockito.when(mockSnmpHelper.getAsync(Mockito.any(SnmpAgentConfig.class), Mockito.any(SnmpObjId[].class)))
                .thenThrow(exception);

        CompletableFuture<ServiceResult> response = target.detect("127.0.0.1", testConfig);

        ServiceResult serviceDetectorResponse = response.get();

        assertFalse(serviceDetectorResponse.getStatus());
        assertEquals(ServiceType.SNMP, serviceDetectorResponse.getService());
        assertEquals(testRequest.getHost(), serviceDetectorResponse.getIpAddress());
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
