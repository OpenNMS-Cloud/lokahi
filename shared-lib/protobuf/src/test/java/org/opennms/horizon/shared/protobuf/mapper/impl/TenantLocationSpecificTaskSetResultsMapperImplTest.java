/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *
 */

package org.opennms.horizon.shared.protobuf.mapper.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Verify the mapper for TaskSetResults <===> TenantLocationSpecificTaskSetResults
 */
public class TenantLocationSpecificTaskSetResultsMapperImplTest {

    private TenantLocationSpecificTaskSetResultsMapperImpl target;

    private TaskResult testTaskResult;

    @Before
    public void setUp() throws Exception {
        target = new TenantLocationSpecificTaskSetResultsMapperImpl();

        // Don't need to fully hydrate this one, it does not get mapped
        testTaskResult =
            TaskResult.newBuilder()
                .setId("x-task-id-x")
                .build();
    }

    @Test
    public void testMapBareToTenanted() {
        //
        // Setup Test Data and Interactions
        //
        TaskSetResults taskSetResults =
            TaskSetResults.newBuilder()
                .addResults(testTaskResult)
                .build();

        //
        // Execute
        //
        TenantLocationSpecificTaskSetResults tenantLocationSpecificTaskSetResults =
            target.mapBareToTenanted("x-tenant-id-x", "x-location-x", taskSetResults);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(taskSetResults, true);
        verifyAllFieldsSet(tenantLocationSpecificTaskSetResults, true);

        assertEquals(1, tenantLocationSpecificTaskSetResults.getResultsCount());
        assertSame(testTaskResult, tenantLocationSpecificTaskSetResults.getResults(0));
    }

    @Test
    public void testMapTenantedToBare() {
        //
        // Setup Test Data and Interactions
        //
        TenantLocationSpecificTaskSetResults tenantLocationSpecificTaskSetResults =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocation("x-location-x")
                .addResults(testTaskResult)
                .build();

        //
        // Execute
        //
        TaskSetResults taskSetResults =
            target.mapTenantedToBare(tenantLocationSpecificTaskSetResults);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(taskSetResults, true);
        verifyAllFieldsSet(tenantLocationSpecificTaskSetResults, true);

        assertEquals(1, tenantLocationSpecificTaskSetResults.getResultsCount());
        assertSame(testTaskResult, tenantLocationSpecificTaskSetResults.getResults(0));
    }

    /**
     * Check for difference in the named fields between the types.
     */
    @Test
    public void testDefinitionsMatch() {
        verifyAllFieldsExceptTenantIdAndLocationMatch(
            TaskSetResults.getDefaultInstance(), TenantLocationSpecificTaskSetResults.getDefaultInstance());
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
                        "message " + typeDescriptor.getFullName() + " has 0 repeated field values for field " + fieldDescriptor.getName() + " (" + fieldDescriptor.getNumber() + ")",
                        message.getRepeatedFieldCount(fieldDescriptor) > 0);
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

        withTenantTypeFields.remove("tenantId");
        withTenantTypeFields.remove("location");

        assertEquals(withTenantTypeFields, withoutTenantTypeFields);
    }
}
