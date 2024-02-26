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
package org.opennms.horizon.shared.flows.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;

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
        FlowDocumentLog testFlowDocumentLog = FlowDocumentLog.newBuilder()
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
        assertEquals("x-location-x", mappedResult.getLocationId());
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
                        .setLocationId("x-location-x")
                        .setSystemId("systemId")
                        .addMessage(FlowDocument.newBuilder())
                        .build();

        //
        // Execute
        //
        FlowDocumentLog mappedResult = target.mapTenantedToBare(testTenantLocationSpecificFlowDocumentLog);

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

    // ========================================
    // Internals
    // ----------------------------------------

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
                            (message.getRepeatedFieldCount(fieldDescriptor) > 0),
                            "message " + typeDescriptor.getFullName() + " has 0 repeated field values for field "
                                    + fieldDescriptor.getName() + " (" + fieldDescriptor.getNumber() + ")");
                }
            } else {
                if (!message.hasField(fieldDescriptor)) {
                    fail("message " + typeDescriptor.getFullName() + " is missing field " + fieldDescriptor.getName()
                            + " (" + fieldDescriptor.getNumber() + ")");
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
    private void verifyAllFieldsExceptTenantIdAndLocationMatch(
            Message messageWithoutTenant, Message messageWithTenant) {
        Descriptors.Descriptor withoutTenantTypeDescriptor = messageWithoutTenant.getDescriptorForType();
        Descriptors.Descriptor withTenantTypeDescriptor = messageWithTenant.getDescriptorForType();

        Set<String> withoutTenantTypeFields = withoutTenantTypeDescriptor.getFields().stream()
                .map(Descriptors.FieldDescriptor::getName)
                .collect(Collectors.toSet());
        Set<String> withTenantTypeFields = withTenantTypeDescriptor.getFields().stream()
                .map(Descriptors.FieldDescriptor::getName)
                .collect(Collectors.toSet());

        withTenantTypeFields.remove("tenant_id");
        withTenantTypeFields.remove("location_id");

        assertEquals(withTenantTypeFields, withoutTenantTypeFields);
    }
}
