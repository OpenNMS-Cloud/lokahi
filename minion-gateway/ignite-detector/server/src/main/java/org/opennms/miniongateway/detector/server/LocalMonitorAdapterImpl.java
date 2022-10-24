package org.opennms.miniongateway.detector.server;

import java.util.concurrent.CompletableFuture;
import org.opennms.miniongateway.detector.api.LocalMonitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalMonitorAdapterImpl implements LocalMonitorAdapter {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(LocalMonitorAdapterImpl.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public CompletableFuture<Boolean> monitor() {

        log.warn("STUBBED MONITOR - NEED TO ROUTE TO MINION VIA GRPC");

        return CompletableFuture.completedFuture(false);
    }
}
