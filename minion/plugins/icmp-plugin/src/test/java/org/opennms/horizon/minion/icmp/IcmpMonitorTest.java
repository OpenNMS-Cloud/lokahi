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
package org.opennms.horizon.minion.icmp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.icmp.contract.IcmpMonitorRequest;

public class IcmpMonitorTest {
    private static final String TEST_LOCALHOST_IP_VALUE = "127.0.0.1";

    @Mock
    MonitoredService monitoredService;

    IcmpMonitorRequest testEchoRequest;
    Any testConfig;
    IcmpMonitor icmpMonitor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        testEchoRequest =
                IcmpMonitorRequest.newBuilder().setHost(TEST_LOCALHOST_IP_VALUE).build();

        testConfig = Any.pack(testEchoRequest);
    }

    private IcmpMonitor getIcmpMonitor(boolean isError, boolean isTimeout) {
        TestPinger testPinger = new TestPinger();
        testPinger.setHandleResponse(!isTimeout);
        testPinger.setHandleTimeout(isTimeout);
        testPinger.setHandleError(isError);

        PingerFactory pingerFactory = Mockito.mock(PingerFactory.class);
        when(pingerFactory.getInstance(anyInt(), anyBoolean())).thenReturn(testPinger);

        return new IcmpMonitor(pingerFactory);
    }

    @Test
    public void poll() throws Exception {
        icmpMonitor = getIcmpMonitor(false, false);
        CompletableFuture<ServiceMonitorResponse> response = icmpMonitor.poll(monitoredService, testConfig);

        ServiceMonitorResponse serviceMonitorResponse = response.get();

        assertEquals(Status.Up, serviceMonitorResponse.getStatus());
        assertTrue(serviceMonitorResponse.getResponseTime() > 0.0);
    }

    @Test
    public void testTimeout() throws Exception {
        icmpMonitor = getIcmpMonitor(false, true);

        CompletableFuture<ServiceMonitorResponse> response = icmpMonitor.poll(monitoredService, testConfig);

        ServiceMonitorResponse serviceMonitorResponse = response.get();

        assertEquals(Status.Unknown, serviceMonitorResponse.getStatus());
        assertEquals("timeout", serviceMonitorResponse.getReason());
        assertEquals(0.0d, serviceMonitorResponse.getResponseTime(), 0);
    }

    @Test
    public void testError() throws Exception {
        icmpMonitor = getIcmpMonitor(true, false);

        CompletableFuture<ServiceMonitorResponse> response = icmpMonitor.poll(monitoredService, testConfig);

        ServiceMonitorResponse serviceMonitorResponse = response.get();

        assertEquals(Status.Down, serviceMonitorResponse.getStatus());
        assertEquals("Failed to ping", serviceMonitorResponse.getReason());
        assertEquals(0.0d, serviceMonitorResponse.getResponseTime(), 0);
    }
}
