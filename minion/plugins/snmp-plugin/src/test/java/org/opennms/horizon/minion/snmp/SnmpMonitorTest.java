package org.opennms.horizon.minion.snmp;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.StrategyResolver;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * TBD888: incomplete
 */
public class SnmpMonitorTest {

    private static final Logger log = LoggerFactory.getLogger(SnmpMonitorTest.class);

    private SnmpMonitor target;

    private StrategyResolver mockStrategyResolver;
    private SnmpHelper mockSnmpHelper;

    private MonitoredService mockMonitoredService;
    private ServiceMonitorResponse mockServiceMonitorResponse;
    SnmpMonitorStrategy.FunctionWithException<String, InetAddress, UnknownHostException> mockInetLookupOperation;
    private CompletableFuture<SnmpValue[]> mockFuture1;
    private CompletableFuture<ServiceMonitorResponse> mockFuture2;
    private CompletableFuture<ServiceMonitorResponse> mockFuture3;
    private SnmpValue mockSnmpValue;

    private SnmpMonitorRequest testRequest;
    private Any testConfig;

    private ArgumentCaptor<Function<? super SnmpValue[], ? extends ServiceMonitorResponse>> processSnmpResponseCaptor;


    @Before
    public void setUp() throws Exception {
        mockStrategyResolver = Mockito.mock(StrategyResolver.class);
        mockSnmpHelper = Mockito.mock(SnmpHelper.class);
        mockMonitoredService = Mockito.mock(MonitoredService.class);

        mockServiceMonitorResponse = Mockito.mock(ServiceMonitorResponse.class);
        mockInetLookupOperation = Mockito.mock(SnmpMonitorStrategy.FunctionWithException.class);
        mockFuture1 = Mockito.mock(CompletableFuture.class);
        mockFuture2 = Mockito.mock(CompletableFuture.class);
        mockFuture3 = Mockito.mock(CompletableFuture.class);
        mockSnmpValue = Mockito.mock(SnmpValue.class);

        testRequest =
            SnmpMonitorRequest.newBuilder()
                .setHost("x-host-addr-x")
                .setCommunity("x-test-community-x")
                .build()
            ;

        testConfig = Any.pack(testRequest);

        target = new SnmpMonitor(mockStrategyResolver, mockSnmpHelper);
        target.setInetLookupOperation(mockInetLookupOperation);

        // TBD888: need to match SnmpAgentConfig
        Mockito.when(mockSnmpHelper.getAsync(Mockito.any(), Mockito.any())).thenReturn(mockFuture1);
        processSnmpResponseCaptor = ArgumentCaptor.forClass(Function.class);
        Mockito.when(mockFuture1.thenApply(processSnmpResponseCaptor.capture())).thenReturn((CompletableFuture) mockFuture2);
        Mockito.when(mockFuture2.orTimeout(3000, TimeUnit.MILLISECONDS)).thenReturn(mockFuture3);
    }

    @Test
    public void testPoll() throws Exception {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        CompletableFuture<ServiceMonitorResponse> future = target.poll(mockMonitoredService, testConfig);

        //
        // Verify the Results
        //
        assertSame(mockFuture3, future);
    }

    @Test
    public void testSnmpProcessResultPollSuccess() throws Exception {
        //
        // Setup Test Data and Interactions
        //  Yes, executing here, but just enough to get the processSnmpResponse function captured.
        //
        target.poll(mockMonitoredService, testConfig);
        Function<? super SnmpValue[], ? extends ServiceMonitorResponse>
            processSnmpResponse = processSnmpResponseCaptor.getValue();

        Mockito.when(mockSnmpValue.isNumeric()).thenReturn(true);
        Mockito.when(mockSnmpValue.toLong()).thenReturn(111000222L);

        //
        // Execute
        //
        ServiceMonitorResponse smr = processSnmpResponse.apply(new SnmpValue[]{ mockSnmpValue });

        //
        // Verify the Results
        //
        assertEquals(ServiceMonitorResponse.Status.Up, smr.getStatus());
        assertNull(smr.getReason());
        assertEquals(111000222L, smr.getProperties().get("observedValue"));
    }

    @Test
    public void testSnmpProcessResultPollFailed() throws Exception {
        //
        // Setup Test Data and Interactions
        //  Yes, executing here, but just enough to get the processSnmpResponse function captured.
        //
        target.poll(mockMonitoredService, testConfig);
        Function<? super SnmpValue[], ? extends ServiceMonitorResponse>
            processSnmpResponse = processSnmpResponseCaptor.getValue();

        //
        // Execute
        //
        ServiceMonitorResponse smr = processSnmpResponse.apply(new SnmpValue[]{ null });

        //
        // Verify the Results
        //
        assertEquals("SNMP poll failed, addr=x-host-addr-x oid=.1.3.6.1.2.1.1.2.0", smr.getReason());
    }
}
