package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Any;

import java.util.concurrent.CompletableFuture;

public interface ServiceDetector {

    CompletableFuture<ServiceDetectorResponse> detect(Any config);

}
