package org.opennms.horizon.minion.plugin.api.registries;

import org.opennms.horizon.minion.plugin.api.ServiceConnectorFactory;

public interface ServiceConnectorFactoryRegistry {

    ServiceConnectorFactory getService(String type);
}
