package org.opennms.horizon.inventory.testtool.miniongateway;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.Test;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.icmp.contract.IcmpMonitorRequest;

/**
 * Developer Test to discover protobuf
 */
public class ProtobufReflectionDT {

    @Test
    void name() {
        Descriptors.Descriptor descriptor = IcmpMonitorRequest.getDescriptor();
        System.out.println("full-name=" + descriptor.getFullName());
        System.out.println("name=" + descriptor.getName());

        System.out.println("type-url=" + Any.pack(IcmpDetectorRequest.newBuilder().build()).getTypeUrl());

        Any any = Any.pack(IcmpDetectorRequest.newBuilder().build());
        System.out.println("any descriptor: name=" + any.getDescriptorForType().getName());
        System.out.println("any descriptor: full-name=" + any.getDescriptorForType().getFullName());
    }
}
