package org.opennms.horizon.events.traps.impl;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.events.traps.EventFactory;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;

import java.net.InetAddress;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class TrapLogProtoToXmlMapperImplTest {

    private EventFactory mockEventFactory;
    private Function<String, InetAddress> mockInetAddressFunction;

    private InetAddress testInetAddress;
    private TrapDTO testTrapDTO;
    private Identity testIdentity;
    private TenantLocationSpecificTrapLogDTO testTenantLocationSpecificTrapLogDTO;
    private Event testXmlEvent;


    private TrapLogProtoToEventLogXmlMapperImpl target;

    @BeforeEach
    public void setUp() {
        mockEventFactory = Mockito.mock(EventFactory.class);
        mockInetAddressFunction = Mockito.mock(Function.class);
        testInetAddress = Mockito.mock(InetAddress.class);

        testTrapDTO =
            TrapDTO.newBuilder()
                .setCommunity("x-community-x")
                .build();

        testIdentity =
            Identity.newBuilder()
                .setSystemId("x-system-id-x")
                .build();

        testTenantLocationSpecificTrapLogDTO =
            TenantLocationSpecificTrapLogDTO.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocation("x-location-x")
                .setIdentity(testIdentity)
                .setTrapAddress("1.2.3.4")
                .addTrapDTO(testTrapDTO)
                .build();

        testXmlEvent = new Event();

        Mockito.when(mockInetAddressFunction.apply("1.2.3.4")).thenReturn(testInetAddress);
        Mockito.when(mockEventFactory.createEventFrom(testTrapDTO, "x-system-id-x", "x-location-x", testInetAddress, "x-tenant-id-x")).thenReturn(testXmlEvent);

        target = new TrapLogProtoToEventLogXmlMapperImpl();

        target.setEventFactory(mockEventFactory);
        target.setInetAddressLookupFunction(mockInetAddressFunction);
    }

    @Test
    void testConvert() {
        //
        // Setup Test Data and Interactions
        //


        //
        // Execute
        //
        Log result = target.convert(testTenantLocationSpecificTrapLogDTO);

        //
        // Verify the Results
        //
        assertEquals(1, result.getEvents().getEventCount());
        assertSame(testXmlEvent, result.getEvents().getEvent(0));
    }

    @Test
    void testConvertReturnsNull() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.reset(mockEventFactory);
        Mockito.when(mockEventFactory.createEventFrom(testTrapDTO, "x-system-id-x", "x-location-x", testInetAddress, "x-tenant-id-x")).thenReturn(null);

        //
        // Execute
        //
        Log result = target.convert(testTenantLocationSpecificTrapLogDTO);

        //
        // Verify the Results
        //
        assertEquals(0, result.getEvents().getEventCount());
    }

    @Test
    void testConvertException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");

        Mockito.reset(mockEventFactory);
        Mockito.when(mockEventFactory.createEventFrom(testTrapDTO, "x-system-id-x", "x-location-x", testInetAddress, "x-tenant-id-x")).thenThrow(testException);

        //
        // Execute
        //
        try (var logCaptor = LogCaptor.forClass(TrapLogProtoToEventLogXmlMapperImpl.class)) {
            //
            // Execute
            //
            Log result = target.convert(testTenantLocationSpecificTrapLogDTO);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) -> (
                        Objects.equals("Unexpected error processing trap: {}", logEvent.getMessage()) &&
                        (logEvent.getArguments().size() == 1) &&
                        ( logEvent.getArguments().get(0) == testTrapDTO ) &&
                        ( logEvent.getThrowable().orElse(null) == testException )
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            assertEquals(0, result.getEvents().getEventCount());
        }
    }
}
