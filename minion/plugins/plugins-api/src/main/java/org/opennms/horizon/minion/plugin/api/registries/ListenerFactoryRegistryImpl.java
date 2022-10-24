package org.opennms.horizon.minion.plugin.api.registries;

import com.savoirtech.eos.pattern.whiteboard.KeyedWhiteboard;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.ListenerFactory;
import org.osgi.framework.BundleContext;

@Slf4j
public class ListenerFactoryRegistryImpl extends KeyedWhiteboard<String, ListenerFactory> implements ListenerFactoryRegistry {

    public static final String PLUGIN_IDENTIFIER = "listener.name";

    public ListenerFactoryRegistryImpl(BundleContext bundleContext) {
        super(bundleContext, ListenerFactory.class, (svc, props) -> props.getProperty(PLUGIN_IDENTIFIER));
    }
}
