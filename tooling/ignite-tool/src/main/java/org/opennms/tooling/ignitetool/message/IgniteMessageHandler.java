package org.opennms.tooling.ignitetool.message;


import org.apache.ignite.lang.IgniteBiPredicate;

import java.util.UUID;
import java.util.function.Consumer;

public class IgniteMessageHandler implements IgniteBiPredicate<UUID, Object> {

    private final Consumer processor;
    private boolean shutdown = false;

    public IgniteMessageHandler(Consumer processor) {
        this.processor = processor;
    }

    public void stop() {
        shutdown = true;
    }

    @Override
    public boolean apply(UUID uuid, Object content) {
        processor.accept(content);
        return ! shutdown;
    }
}
