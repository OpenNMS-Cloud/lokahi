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

        Date timestamp = new Date(heartbeat.getTimestamp().getSeconds());
        heartbeatConsumer.update(heartbeat.getIdentity().getSystemId(), heartbeat.getIdentity().getLocation(), timestamp);
    }
}
