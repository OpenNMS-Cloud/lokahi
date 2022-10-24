package org.opennms.miniongateway.detector.api;

import java.util.concurrent.CompletableFuture;

public interface LocalMonitorAdapter {
    CompletableFuture<Boolean> monitor();
}
