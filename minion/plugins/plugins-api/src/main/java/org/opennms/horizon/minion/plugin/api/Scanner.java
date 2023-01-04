package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Any;

import java.util.concurrent.CompletableFuture;

public interface Scanner {
    CompletableFuture<AzureScannerResponse> scan(Any config);
}
