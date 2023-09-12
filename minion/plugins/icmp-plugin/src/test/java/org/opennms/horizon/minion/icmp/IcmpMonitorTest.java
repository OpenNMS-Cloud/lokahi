package org.opennms.horizon.minion.icmp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;

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
            IcmpMonitorRequest.newBuilder()
                .setHost(TEST_LOCALHOST_IP_VALUE)
                .build();

        testConfig = Any.pack(testEchoRequest);
    }

    private IcmpMonitor getIcmpMonitor(boolean isError, boolean isTimeout) {
        TestPinger testPinger = new TestPinger();
        testPinger.setHandleResponse(!isTimeout);
        testPinger.setHandleTimeout(isTimeout);
        testPinger.setHandleError(isError);

        PingerFactory pingerFactory = Mockito.mock(PingerFactory.class);
        when(pingerFactory.getInstance(anyInt(), anyBoolean()))
            .thenReturn(testPinger);

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
