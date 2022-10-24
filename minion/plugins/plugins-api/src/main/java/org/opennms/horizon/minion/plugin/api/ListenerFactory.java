package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Any;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface for a ListenerFactory, which constructs Listeners.
 */
public interface ListenerFactory {
    Listener create(Consumer<ServiceMonitorResponse> resultProcessor, Any config);
}
