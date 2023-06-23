package org.opennms.horizon.grpc.heartbeat.contract.mapper.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TenantLocationSpecificHeartbeatMessageMapperImplTest {

    private TenantLocationSpecificHeartbeatMessageMapperImpl target;

    @BeforeEach
    public void setUp() {
        target = new TenantLocationSpecificHeartbeatMessageMapperImpl();
    }

    @Test
    public void testMapBareToTenantLocationSpecific() {
        //
        // Setup Test Data and Interactions
        //
        HeartbeatMessage testHeartbeatMessage =
            HeartbeatMessage.newBuilder()
                .setIdentity(
                    Identity.newBuilder()
                        .setSystemId("x-system-id-x")
                        .build()
                )
                .setTimestamp(
                    Timestamp.newBuilder()
                        .setSeconds(123123)
                        .setNanos(456456)
                        .build()
                )
                .build();

        //
        // Execute
        //
        TenantLocationSpecificHeartbeatMessage mappedResult =
            target.mapBareToTenanted("x-tenant-id-x", "x-location-x", testHeartbeatMessage);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(testHeartbeatMessage, true);
        verifyAllFieldsSet(mappedResult, true);

        assertEquals("x-tenant-id-x", mappedResult.getTenantId());
        assertEquals("x-location-x", mappedResult.getLocationId());
        assertEquals(123123, mappedResult.getTimestamp().getSeconds());
        assertEquals(456456, mappedResult.getTimestamp().getNanos());
    }

    @Test
    public void testMapTenantedToBare() {
        //
        // Setup Test Data and Interactions
        //
        TenantLocationSpecificHeartbeatMessage tenantLocationSpecificHeartbeatMessage =
            TenantLocationSpecificHeartbeatMessage.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .setIdentity(Identity.newBuilder().setSystemId("x-system-id-x"))
                .setTimestamp(
                    Timestamp.newBuilder()
                        .setSeconds(123123)
                        .setNanos(456456)
                        .build()
                )
                .build();

        //
        // Execute
        //
        HeartbeatMessage mappedResult = target.mapTenantedToBare(tenantLocationSpecificHeartbeatMessage);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(mappedResult, true);
        verifyAllFieldsSet(tenantLocationSpecificHeartbeatMessage, true);

        assertEquals("x-system-id-x", mappedResult.getIdentity().getSystemId());
        assertEquals(123123, mappedResult.getTimestamp().getSeconds());
        assertEquals(456456, mappedResult.getTimestamp().getNanos());
    }

    /**
     * Check for difference in the named fields between the types.
     */
    @Test
    public void testDefinitionsMatch() {
        verifyAllFieldsExceptTenantIdAndLocationMatch(
            HeartbeatMessage.getDefaultInstance(), TenantLocationSpecificHeartbeatMessage.getDefaultInstance());
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Verify all of the fields in the given message have been set to help ensure completeness of the test.
     *
     * @param message the message for which fields will be verified.
     * @param repeatedMustNotBeEmpty true => verify repeated fields have at least one element; false => ignore repeated
     *                               fields.  Unfortunately there is no concept of "not set" for repeated fields - they
     *                               are always "non-null".
     */
    private void verifyAllFieldsSet(Message message, boolean repeatedMustNotBeEmpty) {
        Descriptors.Descriptor typeDescriptor = message.getDescriptorForType();

        List<Descriptors.FieldDescriptor> fieldDescriptorList = typeDescriptor.getFields();

        //
        // IF YOU SEE FAILURE HERE, MAKE SURE BOTH THE TEST AND THE MAPPER ARE INCLUDING ALL FIELDS
        //
        for (var fieldDescriptor : fieldDescriptorList) {
            if (fieldDescriptor.isRepeated()) {
                if (repeatedMustNotBeEmpty) {
                    assertTrue(
                        ( message.getRepeatedFieldCount(fieldDescriptor) > 0 ),
                        "message " + typeDescriptor.getFullName() + " has 0 repeated field values for field " + fieldDescriptor.getName() + " (" + fieldDescriptor.getNumber() + ")"
                        );
                }
            } else {
                if (!message.hasField(fieldDescriptor)) {
                    fail("message " + typeDescriptor.getFullName() + " is missing field " + fieldDescriptor.getName() + " (" + fieldDescriptor.getNumber() + ")");
                }
            }
        }
    }

    /**
     * Verify both message types have the same fields except for tenant id.
     *
     * @param messageWithoutTenant
     * @param messageWithTenant
     */
    private void verifyAllFieldsExceptTenantIdAndLocationMatch(Message messageWithoutTenant, Message messageWithTenant) {
        Descriptors.Descriptor withoutTenantTypeDescriptor = messageWithoutTenant.getDescriptorForType();
        Descriptors.Descriptor withTenantTypeDescriptor = messageWithTenant.getDescriptorForType();

        Set<String> withoutTenantTypeFields =
            withoutTenantTypeDescriptor.getFields().stream().map(Descriptors.FieldDescriptor::getName).collect(Collectors.toSet());
        Set<String> withTenantTypeFields =
            withTenantTypeDescriptor.getFields().stream().map(Descriptors.FieldDescriptor::getName).collect(Collectors.toSet());

        withTenantTypeFields.remove("tenant_id");
        withTenantTypeFields.remove("location_id");

        assertEquals(withTenantTypeFields, withoutTenantTypeFields);
    }
}
