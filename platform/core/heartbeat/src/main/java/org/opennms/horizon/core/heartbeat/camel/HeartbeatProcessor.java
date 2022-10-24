package org.opennms.horizon.core.heartbeat.camel;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.horizon.core.heartbeat.HeartbeatConsumer;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;

import java.util.Date;

/**
 * Camel processor that passes heartbeat updates to the heartbeat consumer.
 *
 * TODO: clean up - don't need HeartbeatConsumer to be a SinkModule any more.
 */
public class HeartbeatProcessor implements Processor {

    @Getter
    @Setter
    private HeartbeatConsumer heartbeatConsumer;

    @Override
    public void process(Exchange exchange) throws Exception {
        HeartbeatMessage heartbeat = exchange.getIn().getMandatoryBody(HeartbeatMessage.class);

        long millis =
            (heartbeat.getTimestamp().getSeconds() * 1_000) +
            (heartbeat.getTimestamp().getNanos() / 1_000_000)
            ;

        Date timestamp = new Date(millis);
        heartbeatConsumer.update(heartbeat.getIdentity().getSystemId(), heartbeat.getIdentity().getLocation(), timestamp);
    }
}
