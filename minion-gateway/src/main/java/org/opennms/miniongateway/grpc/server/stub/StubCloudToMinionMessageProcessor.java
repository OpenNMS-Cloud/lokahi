package org.opennms.miniongateway.grpc.server.stub;

import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.MinionHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class StubCloudToMinionMessageProcessor implements BiConsumer<MinionHeader, StreamObserver<CloudToMinionMessage>> {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(StubCloudToMinionMessageProcessor.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public void accept(MinionHeader minionHeader, StreamObserver<CloudToMinionMessage> cloudToMinionMessageStreamObserver) {
        log.info("Have Message to send to Minion: system-id={}, location={}",
            minionHeader.getSystemId(),
            minionHeader.getLocation());
    }
}
