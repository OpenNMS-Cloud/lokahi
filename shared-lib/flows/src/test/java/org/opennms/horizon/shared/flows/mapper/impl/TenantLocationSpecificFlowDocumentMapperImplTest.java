package org.opennms.horizon.shared.flows.mapper.impl;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Message;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.flows.document.Direction;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.Locality;
import org.opennms.horizon.flows.document.NetflowVersion;
import org.opennms.horizon.flows.document.NodeInfo;
import org.opennms.horizon.flows.document.SamplingAlgorithm;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TenantLocationSpecificFlowDocumentMapperImplTest {
    private TenantLocationSpecificFlowDocumentMapperImpl target;

    @BeforeEach
    public void setUp() throws Exception {
        target = new TenantLocationSpecificFlowDocumentMapperImpl();
    }

    @Test
    public void testMapBareToTenantLocationSpecific() {
        //
        // Setup Test Data and Interactions
        //
        FlowDocument testFlowDocument =
            FlowDocument.newBuilder()
                .setTimestamp(123123)
                .setNumBytes(UInt64Value.of(1313L))
                .setDirection(Direction.EGRESS)
                .setDstAddress("x-dst_address-x")
                .setDstHostname("x-dst_hostname-x")
                .setDstAs(UInt64Value.of(17170000L))
                .setDstMaskLen(UInt32Value.of(17170001))
                .setDstPort(UInt32Value.of(17170002))
                .setEngineId(UInt32Value.of(17170003))
                .setEngineType(UInt32Value.of(17170004))
                .setDeltaSwitched(UInt64Value.of(17170005L))
                .setFirstSwitched(UInt64Value.of(17170006L))
                .setLastSwitched(UInt64Value.of(17170007L))
                .setNumFlowRecords(UInt32Value.of(17170008))
                .setNumPackets(UInt64Value.of(17170009L))
                .setFlowSeqNum(UInt64Value.of(17170010L))
                .setInputSnmpIfindex(UInt32Value.of(17170011))
                .setOutputSnmpIfindex(UInt32Value.of(17170012))
                .setIpProtocolVersion(UInt32Value.of(17170013))
                .setNextHopAddress("x-next_hop_address-x")
                .setNextHopHostname("x-next_hop_hostname-x")
                .setProtocol(UInt32Value.of(17170014))
                .setSamplingAlgorithm(SamplingAlgorithm.HASH_BASED_FILTERING)
                .setSamplingInterval(DoubleValue.of(1.001002))
                .setSrcAddress("x-src_address-x")
                .setSrcHostname("x-src_hostname-x")
                .setSrcAs(UInt64Value.of(17170015L))
                .setSrcMaskLen(UInt32Value.of(17170016))
                .setSrcPort(UInt32Value.of(17170017))
                .setTcpFlags(UInt32Value.of(17170018))
                .setTos(UInt32Value.of(17170019))
                .setNetflowVersion(NetflowVersion.IPFIX)
                .setVlan(UInt32Value.of(17170020))
                .setSrcNode(NodeInfo.newBuilder().build())
                .setExporterNode(NodeInfo.newBuilder().build())
                .setDestNode(NodeInfo.newBuilder().build())
                .setApplication("x-application-x")
                .setHost("x-host-x")
                .setSrcLocality(Locality.PRIVATE)
                .setDstLocality(Locality.PRIVATE)
                .setFlowLocality(Locality.PRIVATE)
                .setClockCorrection(7654321)
                .setDscp(UInt32Value.of(17170021))
                .setEcn(UInt32Value.of(17170022))
                .setExporterAddress("x-exporter_address-x")
                .setExporterPort(UInt32Value.of(17170023))
                .setExporterIdentifier("x-exporter_identifier-x")
                .setReceivedAt(456456456L)
                .build();

        //
        // Execute
        //
        TenantLocationSpecificFlowDocument mappedResult =
            target.mapBareToTenanted("x-tenant-id-x", "x-location-x", testFlowDocument);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(testFlowDocument, true);
        verifyAllFieldsSet(mappedResult, true);

        assertEquals("x-tenant-id-x", mappedResult.getTenantId());
        assertEquals("x-location-x", mappedResult.getLocation());
        assertEquals(123123, mappedResult.getTimestamp());
        assertEquals(1313L, mappedResult.getNumBytes().getValue());
        assertEquals(Direction.EGRESS, mappedResult.getDirection());
        assertEquals("x-dst_address-x", mappedResult.getDstAddress());
        assertEquals("x-dst_hostname-x", mappedResult.getDstHostname());
        assertEquals(17170000L, mappedResult.getDstAs().getValue());
        assertEquals(17170001, mappedResult.getDstMaskLen().getValue());
        assertEquals(17170002, mappedResult.getDstPort().getValue());
        assertEquals(17170003, mappedResult.getEngineId().getValue());
        assertEquals(17170004, mappedResult.getEngineType().getValue());
        assertEquals(17170005L, mappedResult.getDeltaSwitched().getValue());
        assertEquals(17170006L, mappedResult.getFirstSwitched().getValue());
        assertEquals(17170007L, mappedResult.getLastSwitched().getValue());
        assertEquals(17170008, mappedResult.getNumFlowRecords().getValue());
        assertEquals(17170009L, mappedResult.getNumPackets().getValue());
        assertEquals(17170010L, mappedResult.getFlowSeqNum().getValue());
        assertEquals(17170011, mappedResult.getInputSnmpIfindex().getValue());
        assertEquals(17170012, mappedResult.getOutputSnmpIfindex().getValue());
        assertEquals(17170013, mappedResult.getIpProtocolVersion().getValue());
        assertEquals("x-next_hop_address-x", mappedResult.getNextHopAddress());
        assertEquals("x-next_hop_hostname-x", mappedResult.getNextHopHostname());
        assertEquals(17170014, mappedResult.getProtocol().getValue());
        assertEquals(SamplingAlgorithm.HASH_BASED_FILTERING, mappedResult.getSamplingAlgorithm());
        assertEquals(1.001002, mappedResult.getSamplingInterval().getValue(), 0.0000000001);
        assertEquals("x-src_address-x", mappedResult.getSrcAddress());
        assertEquals("x-src_hostname-x", mappedResult.getSrcHostname());
        assertEquals(17170015L, mappedResult.getSrcAs().getValue());
        assertEquals(17170016, mappedResult.getSrcMaskLen().getValue());
        assertEquals(17170017, mappedResult.getSrcPort().getValue());
        assertEquals(17170018, mappedResult.getTcpFlags().getValue());
        assertEquals(17170019, mappedResult.getTos().getValue());
        assertEquals(NetflowVersion.IPFIX, mappedResult.getNetflowVersion());
        assertEquals(17170020, mappedResult.getVlan().getValue());
        assertEquals(testFlowDocument.getSrcNode(), mappedResult.getSrcNode());
        assertEquals(testFlowDocument.getExporterNode(), mappedResult.getExporterNode());
        assertEquals(testFlowDocument.getDestNode(), mappedResult.getDestNode());
        assertEquals("x-application-x", mappedResult.getApplication());
        assertEquals("x-host-x", mappedResult.getHost());
        assertEquals(Locality.PRIVATE, mappedResult.getSrcLocality());
        assertEquals(Locality.PRIVATE, mappedResult.getDstLocality());
        assertEquals(Locality.PRIVATE, mappedResult.getFlowLocality());
        assertEquals(7654321, mappedResult.getClockCorrection());
        assertEquals(17170021, mappedResult.getDscp().getValue());
        assertEquals(17170022, mappedResult.getEcn().getValue());
        assertEquals("x-exporter_address-x", mappedResult.getExporterAddress());
        assertEquals(17170023, mappedResult.getExporterPort().getValue());
        assertEquals("x-exporter_identifier-x", mappedResult.getExporterIdentifier());
        assertEquals(456456456L, mappedResult.getReceivedAt());
    }

    @Test
    public void testMapTenantedToBare() {
        //
        // Setup Test Data and Interactions
        //
        TenantLocationSpecificFlowDocument testTenantLocationSpecificFlowDocument =
            TenantLocationSpecificFlowDocument.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocation("x-location-x")

                .setTimestamp(123123)
                .setNumBytes(UInt64Value.of(1313L))
                .setDirection(Direction.EGRESS)
                .setDstAddress("x-dst_address-x")
                .setDstHostname("x-dst_hostname-x")
                .setDstAs(UInt64Value.of(17170000L))
                .setDstMaskLen(UInt32Value.of(17170001))
                .setDstPort(UInt32Value.of(17170002))
                .setEngineId(UInt32Value.of(17170003))
                .setEngineType(UInt32Value.of(17170004))
                .setDeltaSwitched(UInt64Value.of(17170005L))
                .setFirstSwitched(UInt64Value.of(17170006L))
                .setLastSwitched(UInt64Value.of(17170007L))
                .setNumFlowRecords(UInt32Value.of(17170008))
                .setNumPackets(UInt64Value.of(17170009L))
                .setFlowSeqNum(UInt64Value.of(17170010L))
                .setInputSnmpIfindex(UInt32Value.of(17170011))
                .setOutputSnmpIfindex(UInt32Value.of(17170012))
                .setIpProtocolVersion(UInt32Value.of(17170013))
                .setNextHopAddress("x-next_hop_address-x")
                .setNextHopHostname("x-next_hop_hostname-x")
                .setProtocol(UInt32Value.of(17170014))
                .setSamplingAlgorithm(SamplingAlgorithm.HASH_BASED_FILTERING)
                .setSamplingInterval(DoubleValue.of(1.001002))
                .setSrcAddress("x-src_address-x")
                .setSrcHostname("x-src_hostname-x")
                .setSrcAs(UInt64Value.of(17170015L))
                .setSrcMaskLen(UInt32Value.of(17170016))
                .setSrcPort(UInt32Value.of(17170017))
                .setTcpFlags(UInt32Value.of(17170018))
                .setTos(UInt32Value.of(17170019))
                .setNetflowVersion(NetflowVersion.IPFIX)
                .setVlan(UInt32Value.of(17170020))
                .setSrcNode(NodeInfo.newBuilder().build())
                .setExporterNode(NodeInfo.newBuilder().build())
                .setDestNode(NodeInfo.newBuilder().build())
                .setApplication("x-application-x")
                .setHost("x-host-x")
                .setSrcLocality(Locality.PRIVATE)
                .setDstLocality(Locality.PRIVATE)
                .setFlowLocality(Locality.PRIVATE)
                .setClockCorrection(7654321)
                .setDscp(UInt32Value.of(17170021))
                .setEcn(UInt32Value.of(17170022))
                .setExporterAddress("x-exporter_address-x")
                .setExporterPort(UInt32Value.of(17170023))
                .setExporterIdentifier("x-exporter_identifier-x")
                .setReceivedAt(456456456L)
                .build();

        //
        // Execute
        //
        FlowDocument mappedResult =
            target.mapTenantedToBare(testTenantLocationSpecificFlowDocument);

        //
        // Verify the Results
        //
        verifyAllFieldsSet(testTenantLocationSpecificFlowDocument, true);
        verifyAllFieldsSet(mappedResult, true);

        assertEquals(123123, mappedResult.getTimestamp());
        assertEquals(1313L, mappedResult.getNumBytes().getValue());
        assertEquals(Direction.EGRESS, mappedResult.getDirection());
        assertEquals("x-dst_address-x", mappedResult.getDstAddress());
        assertEquals("x-dst_hostname-x", mappedResult.getDstHostname());
        assertEquals(17170000L, mappedResult.getDstAs().getValue());
        assertEquals(17170001, mappedResult.getDstMaskLen().getValue());
        assertEquals(17170002, mappedResult.getDstPort().getValue());
        assertEquals(17170003, mappedResult.getEngineId().getValue());
        assertEquals(17170004, mappedResult.getEngineType().getValue());
        assertEquals(17170005L, mappedResult.getDeltaSwitched().getValue());
        assertEquals(17170006L, mappedResult.getFirstSwitched().getValue());
        assertEquals(17170007L, mappedResult.getLastSwitched().getValue());
        assertEquals(17170008, mappedResult.getNumFlowRecords().getValue());
        assertEquals(17170009L, mappedResult.getNumPackets().getValue());
        assertEquals(17170010L, mappedResult.getFlowSeqNum().getValue());
        assertEquals(17170011, mappedResult.getInputSnmpIfindex().getValue());
        assertEquals(17170012, mappedResult.getOutputSnmpIfindex().getValue());
        assertEquals(17170013, mappedResult.getIpProtocolVersion().getValue());
        assertEquals("x-next_hop_address-x", mappedResult.getNextHopAddress());
        assertEquals("x-next_hop_hostname-x", mappedResult.getNextHopHostname());
        assertEquals(17170014, mappedResult.getProtocol().getValue());
        assertEquals(SamplingAlgorithm.HASH_BASED_FILTERING, mappedResult.getSamplingAlgorithm());
        assertEquals(1.001002, mappedResult.getSamplingInterval().getValue(), 0.0000000001);
        assertEquals("x-src_address-x", mappedResult.getSrcAddress());
        assertEquals("x-src_hostname-x", mappedResult.getSrcHostname());
        assertEquals(17170015L, mappedResult.getSrcAs().getValue());
        assertEquals(17170016, mappedResult.getSrcMaskLen().getValue());
        assertEquals(17170017, mappedResult.getSrcPort().getValue());
        assertEquals(17170018, mappedResult.getTcpFlags().getValue());
        assertEquals(17170019, mappedResult.getTos().getValue());
        assertEquals(NetflowVersion.IPFIX, mappedResult.getNetflowVersion());
        assertEquals(17170020, mappedResult.getVlan().getValue());
        assertEquals(testTenantLocationSpecificFlowDocument.getSrcNode(), mappedResult.getSrcNode());
        assertEquals(testTenantLocationSpecificFlowDocument.getExporterNode(), mappedResult.getExporterNode());
        assertEquals(testTenantLocationSpecificFlowDocument.getDestNode(), mappedResult.getDestNode());
        assertEquals("x-application-x", mappedResult.getApplication());
        assertEquals("x-host-x", mappedResult.getHost());
        assertEquals(Locality.PRIVATE, mappedResult.getSrcLocality());
        assertEquals(Locality.PRIVATE, mappedResult.getDstLocality());
        assertEquals(Locality.PRIVATE, mappedResult.getFlowLocality());
        assertEquals(7654321, mappedResult.getClockCorrection());
        assertEquals(17170021, mappedResult.getDscp().getValue());
        assertEquals(17170022, mappedResult.getEcn().getValue());
        assertEquals("x-exporter_address-x", mappedResult.getExporterAddress());
        assertEquals(17170023, mappedResult.getExporterPort().getValue());
        assertEquals("x-exporter_identifier-x", mappedResult.getExporterIdentifier());
        assertEquals(456456456L, mappedResult.getReceivedAt());
    }

    /**
     * Check for difference in the named fields between the types.
     */
    @Test
    public void testDefinitionsMatch() {
        verifyAllFieldsExceptTenantIdAndLocationMatch(
            FlowDocument.getDefaultInstance(), TenantLocationSpecificFlowDocument.getDefaultInstance());
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
