/**
 * This file is part of OpenNMS(R).
 * <p>
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p>
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 * http://www.gnu.org/licenses/
 * <p>
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 **/
package org.opennms.horizon.shared.flows.mapper.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TenantLocationSpecificFlowDocumentLogMapperImplTest {
    private TenantLocationSpecificFlowDocumentLogMapperImpl target;

    @BeforeEach
    public void setUp() throws Exception {
        target = new TenantLocationSpecificFlowDocumentLogMapperImpl();
    }

    @Test
    public void testMapBareToTenantLocationSpecific() {
        //
        // Setup Test Data and Interactions
        //
        FlowDocumentLog testFlowDocumentLog =
            FlowDocumentLog.newBuilder()
                .setSystemId("systemId")
                .addMessage(FlowDocument.newBuilder())
                .build();

        //
        // Execute
        //
        TenantLocationSpecificFlowDocumentLog mappedResult =
            target.mapBareToTenanted("x-tenant-id-x", "x-location-x", testFlowDocumentLog);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(testFlowDocumentLog, true);
        verifyAllFieldsSet(mappedResult, true);

        assertEquals("x-tenant-id-x", mappedResult.getTenantId());
        assertEquals("x-location-x", mappedResult.getLocation());
        assertEquals("systemId", mappedResult.getSystemId());
        assertEquals(1, mappedResult.getMessageCount());
    }

    @Test
    public void testMapTenantedToBare() {
        //
        // Setup Test Data and Interactions
        //
        TenantLocationSpecificFlowDocumentLog testTenantLocationSpecificFlowDocumentLog =
            TenantLocationSpecificFlowDocumentLog.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocation("x-location-x")
                .setSystemId("systemId")
                .addMessage(FlowDocument.newBuilder())
                .build();


        //
        // Execute
        //
        FlowDocumentLog mappedResult =
            target.mapTenantedToBare(testTenantLocationSpecificFlowDocumentLog);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(testTenantLocationSpecificFlowDocumentLog, true);
        verifyAllFieldsSet(mappedResult, true);

        assertEquals("systemId", mappedResult.getSystemId());
        assertEquals(1, mappedResult.getMessageCount());
    }

    /**
     * Check for difference in the named fields between the types.
     */
    @Test
    public void testDefinitionsMatch() {
        verifyAllFieldsExceptTenantIdAndLocationMatch(
            FlowDocumentLog.getDefaultInstance(), TenantLocationSpecificFlowDocumentLog.getDefaultInstance());
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
        withTenantTypeFields.remove("location");

        assertEquals(withTenantTypeFields, withoutTenantTypeFields);
    }


}
