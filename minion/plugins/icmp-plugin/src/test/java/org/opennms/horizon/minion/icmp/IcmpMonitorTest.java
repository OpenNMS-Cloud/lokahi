package org.opennms.horizon.minion.icmp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opennms.echo.contract.EchoRequest;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.minion.icmp.best.BestMatchPingerFactory;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;

public class IcmpMonitorTest {
    @Mock
    MonitoredService monitoredService;

    EchoRequest testEchoRequest;
    Any testConfig;
    IcmpMonitor icmpMonitor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(monitoredService.getAddress()).thenReturn(InetAddressUtils.addr("127.0.0.1"));
        icmpMonitor = new IcmpMonitor(new BestMatchPingerFactory());

        testEchoRequest =
            EchoRequest.newBuilder()
                .setHost("127.0.0.1")
                .build();

        testConfig = Any.pack(testEchoRequest);
    }

    @Test
    public void poll() throws Exception {
        CompletableFuture<ServiceMonitorResponse> response = icmpMonitor.poll(monitoredService, testConfig);

        ServiceMonitorResponse serviceMonitorResponse = response.get();

        assertEquals(Status.Up, serviceMonitorResponse.getStatus());
        assertTrue(serviceMonitorResponse.getResponseTime() > 0.0);
    }
}
