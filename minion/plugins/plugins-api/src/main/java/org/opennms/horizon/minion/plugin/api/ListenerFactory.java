package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Any;

/**
 * Interface for a ListenerFactory, which constructs Listeners.
 */
public interface ListenerFactory {
    Listener create(Any config);
}
