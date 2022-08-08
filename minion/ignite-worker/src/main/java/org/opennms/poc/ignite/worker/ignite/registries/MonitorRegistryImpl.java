package org.opennms.poc.ignite.worker.ignite.registries;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.opennms.poc.alerting.RegistrationService;
import org.opennms.poc.plugin.api.ServiceMonitorManager;
import org.osgi.framework.BundleContext;

@Slf4j
public class MonitorRegistryImpl extends AlertingPluginRegistry<String, ServiceMonitorManager> implements MonitorRegistry {

    public static final String PLUGIN_IDENTIFIER = "monitor.name";

    public MonitorRegistryImpl(BundleContext bundleContext, RegistrationService registrationService) {
        super(bundleContext, ServiceMonitorManager.class,PLUGIN_IDENTIFIER, registrationService);
    }

    @Override
    public Map<String, ServiceMonitorManager> getServices() {
        return super.asMap();
    }
}
