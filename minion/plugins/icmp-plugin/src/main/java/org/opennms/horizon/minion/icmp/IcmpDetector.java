package org.opennms.horizon.minion.icmp;

import com.google.protobuf.Any;
import lombok.AllArgsConstructor;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponseImpl;
import org.opennms.taskset.contract.MonitorType;

import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class IcmpDetector implements ServiceDetector {
    @Override
    public CompletableFuture<ServiceDetectorResponse> detect(Any config) {
//        todo: implement this
        return CompletableFuture.completedFuture(ServiceDetectorResponseImpl.builder()
            .monitorType(MonitorType.ICMP).serviceDetected(true).build());
    }
}
