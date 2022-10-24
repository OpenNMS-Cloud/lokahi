package org.opennms.horizon.minion.plugin.api.registries;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.osgi.framework.BundleContext;

@Slf4j
public class DetectorRegistryImpl extends AlertingPluginRegistry<String, ServiceDetectorManager> implements DetectorRegistry {

    public static final String PLUGIN_IDENTIFIER = "detector.name";

    public DetectorRegistryImpl(BundleContext bundleContext, RegistrationService registrationService) {
        super(bundleContext, ServiceDetectorManager.class, PLUGIN_IDENTIFIER, registrationService);
    }

    @Override
    public Map<String, ServiceDetectorManager> getServices() {
        return super.asMap();
    }
}
