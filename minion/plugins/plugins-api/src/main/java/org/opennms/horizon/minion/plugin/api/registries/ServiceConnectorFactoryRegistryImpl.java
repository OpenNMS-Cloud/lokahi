package org.opennms.horizon.minion.plugin.api.registries;

import com.savoirtech.eos.pattern.whiteboard.KeyedWhiteboard;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.ServiceConnectorFactory;
import org.osgi.framework.BundleContext;

@Slf4j
public class ServiceConnectorFactoryRegistryImpl extends KeyedWhiteboard<String, ServiceConnectorFactory> implements ServiceConnectorFactoryRegistry {

    public static final String PLUGIN_IDENTIFIER = "connector.name";

    public ServiceConnectorFactoryRegistryImpl(BundleContext bundleContext) {
        super(bundleContext, ServiceConnectorFactory.class, (svc, props) -> props.getProperty(PLUGIN_IDENTIFIER));
    }
}
