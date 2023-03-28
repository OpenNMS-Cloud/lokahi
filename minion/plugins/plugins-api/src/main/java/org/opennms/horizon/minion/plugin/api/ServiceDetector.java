package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Any;
import org.opennms.node.scan.contract.ServiceResult;

import java.util.concurrent.CompletableFuture;

public interface ServiceDetector {

    CompletableFuture<ServiceDetectorResponse> detect(Any config, long nodeId);

    CompletableFuture<ServiceResult> detect(String host, Any config);

}
