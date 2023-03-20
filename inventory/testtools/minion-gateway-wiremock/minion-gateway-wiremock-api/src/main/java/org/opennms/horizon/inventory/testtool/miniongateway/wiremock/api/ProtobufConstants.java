package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.api;

import com.google.protobuf.Descriptors;
import org.opennms.azure.contract.AzureCollectorRequest;
import org.opennms.azure.contract.AzureMonitorRequest;
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.sink.traps.contract.TrapConfig;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.snmp.contract.SnmpMonitorRequest;

import java.util.List;
import org.opennms.taskset.contract.Resilience;
import org.opennms.taskset.contract.TaskContext;

public interface ProtobufConstants {

    List<Descriptors.Descriptor> PROTOBUF_TYPE_LIST =
        List.of(
            IcmpMonitorRequest.getDescriptor(),
            SnmpMonitorRequest.getDescriptor(),

            IcmpDetectorRequest.getDescriptor(),
            SnmpDetectorRequest.getDescriptor(),

            SnmpCollectorRequest.getDescriptor(),

            NodeScanRequest.getDescriptor(),
            FlowsConfig.getDescriptor(),
            TrapConfig.getDescriptor(),

            AzureScanRequest.getDescriptor(),
            AzureMonitorRequest.getDescriptor(),
            AzureCollectorRequest.getDescriptor(),

            TaskContext.getDescriptor(),
            Resilience.getDescriptor()
        );

}
