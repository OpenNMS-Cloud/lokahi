package org.opennms.horizon.minion.icmp;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.inventory.types.ServiceType;
import org.opennms.minion.icmp.best.BestMatchPingerFactory;
import org.opennms.node.scan.contract.ServiceResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IcmpDetectorTest {
    private static final String TEST_LOCALHOST_IP_VALUE = "127.0.0.1";
    private IcmpDetector target;
    private IcmpDetectorRequest testRequest;
    private Any testConfig;
    private PingerFactory pingerFactory;

    @Before
    public void setUp() {
        testRequest =
            IcmpDetectorRequest.newBuilder()
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

        when(pingerFactory.getInstance(Mockito.anyInt(), Mockito.anyBoolean()))
            .thenReturn(testPinger);

        CompletableFuture<ServiceResult> response = target.detect(TEST_LOCALHOST_IP_VALUE, testConfig);
        ServiceResult serviceDetectorResponse = response.get();

        assertTrue(serviceDetectorResponse.getStatus());
        assertEquals(ServiceType.ICMP, serviceDetectorResponse.getService());
        assertEquals(testRequest.getHost(), serviceDetectorResponse.getIpAddress());
    }
}
