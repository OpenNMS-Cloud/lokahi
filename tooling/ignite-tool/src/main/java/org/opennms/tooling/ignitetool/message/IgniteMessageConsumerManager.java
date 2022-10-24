package org.opennms.tooling.ignitetool.message;

import org.apache.ignite.Ignite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple message consumer manager.  Only one processor can be registered to listen on each topic; attempts to register
 * more are ignored.
 */
@Component
public class IgniteMessageConsumerManager {

    @Autowired
    private Ignite ignite;

    private Map<String, IgniteMessageHandler> messageProcessors = new HashMap<>();

    private Object lock = new Object();

    public void startListenMessages(String topic, Consumer messageProcessor) {
        IgniteMessageHandler processor = new IgniteMessageHandler(messageProcessor);

        synchronized (lock) {
            if (messageProcessors.putIfAbsent(topic, processor) == null) {
                ignite.message().localListen(topic, processor);
            }
        }
    }

    public void stopListenMessages(String topic) {
        IgniteMessageHandler processor;

        synchronized (lock) {
            processor = messageProcessors.remove(topic);
        }

        if (processor != null) {
            ignite.message().stopLocalListen(topic, processor);
        }
    }
}
