package org.opennms.horizon.minion.plugin.api.registries;

import org.opennms.horizon.minion.plugin.api.ListenerFactory;

public interface ListenerFactoryRegistry {

    ListenerFactory getService(String type);
}
