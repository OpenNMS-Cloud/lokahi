package org.opennms.horizon.minion.grpc.client.message;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.horizon.minion.grpc.CloudMessageHandler;
import org.opennms.horizon.shared.ipc.rpc.api.minion.CloudMessageReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThinCloudMessageHandler implements CloudMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(ThinCloudMessageHandler.class);
    private final Set<CloudMessageReceiver> handlers = new CopyOnWriteArraySet<>();

    @Override
    public void handle(CloudToMinionMessage message) {
        if (handlers.isEmpty()) {
            logger.warn("No registered handlers for message {}", message);
            return;
        }

        for (CloudMessageReceiver handler : handlers) {
            if (handler.canHandle(message)) {
                handler.handle(message);
            }
        }
    }

    public void bind(CloudMessageReceiver handler) {
        if (handler != null) {
            handlers.add(handler);
            logger.info("Registered cloud message handler {}", handler);
        }
    }

    public void unbind(CloudMessageReceiver handler) {
        if (handler != null) {
            handlers.remove(handler);
            logger.info("Removing cloud message handler {}", handler);
        }
    }
}
