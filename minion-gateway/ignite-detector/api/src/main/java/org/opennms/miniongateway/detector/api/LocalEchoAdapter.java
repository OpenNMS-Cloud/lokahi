package org.opennms.miniongateway.detector.api;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter which connects Ignite-executed operations to the server's internal processing.
 */
public interface LocalEchoAdapter {
    CompletableFuture<Boolean> echo(String location, String systemId);
}
