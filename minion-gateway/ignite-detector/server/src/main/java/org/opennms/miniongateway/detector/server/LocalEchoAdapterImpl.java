package org.opennms.miniongateway.detector.server;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import org.opennms.miniongateway.detector.api.LocalEchoAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalEchoAdapterImpl implements LocalEchoAdapter {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(LocalEchoAdapterImpl.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public CompletableFuture<Boolean> echo(String location, String systemId) {

        log.warn("STUBBED DETECTOR - NEED TO ROUTE TO MINION VIA GRPC");

        return CompletableFuture.completedFuture(true);
    }
}
