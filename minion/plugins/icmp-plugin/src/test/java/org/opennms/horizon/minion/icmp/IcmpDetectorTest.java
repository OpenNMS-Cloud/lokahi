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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.inventory.types.ServiceType;
import org.opennms.minion.icmp.best.BestMatchPingerFactory;
import org.opennms.node.scan.contract.ServiceResult;

public class IcmpDetectorTest {
    private static final String TEST_LOCALHOST_IP_VALUE = "127.0.0.1";
    private IcmpDetector target;
    private IcmpDetectorRequest testRequest;
    private Any testConfig;
    private PingerFactory pingerFactory;

    @Before
    public void setUp() {
        testRequest = IcmpDetectorRequest.newBuilder()
                .setHost(TEST_LOCALHOST_IP_VALUE)
                .build();

        testConfig = Any.pack(testRequest);
        pingerFactory = mock(BestMatchPingerFactory.class);
        target = new IcmpDetector(pingerFactory);
    }

    @Test
    public void testDetect() throws Exception {
        TestPinger testPinger = new TestPinger();
        testPinger.setHandleResponse(true);

        when(pingerFactory.getInstance(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(testPinger);

        CompletableFuture<ServiceResult> response = target.detect(TEST_LOCALHOST_IP_VALUE, testConfig);
        ServiceResult serviceDetectorResponse = response.get();

        assertTrue(serviceDetectorResponse.getStatus());
        assertEquals(ServiceType.ICMP, serviceDetectorResponse.getService());
        assertEquals(testRequest.getHost(), serviceDetectorResponse.getIpAddress());
    }
}
