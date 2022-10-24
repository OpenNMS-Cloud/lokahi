package org.opennms.miniongateway.detector.server;

import org.opennms.miniongateway.detector.api.LocalDetectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

// TODO: implement the real logic and rename once this is no longer a stub
public class LocalDetectorAdapterStubImpl implements LocalDetectorAdapter {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(LocalDetectorAdapterStubImpl.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public CompletableFuture<Boolean> detect(
        String location,
        String systemId,
        String serviceName,
        String detectorName,
        InetAddress inetAddress,
        Integer nodeId) {

        log.warn("STUBBED DETECTOR - NEED TO ROUTE TO MINION VIA GRPC");

        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            log.info("################## CompletableFuture test! returning true");
            return true;
        });
    }
}
