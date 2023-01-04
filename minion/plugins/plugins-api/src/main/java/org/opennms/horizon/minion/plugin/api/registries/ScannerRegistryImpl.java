package org.opennms.horizon.minion.plugin.api.registries;

import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.osgi.framework.BundleContext;

import java.util.Map;

@Slf4j
public class ScannerRegistryImpl extends AlertingPluginRegistry<String, ScannerManager> implements ScannerRegistry {

    public static final String PLUGIN_IDENTIFIER = "scanner.name";

    public ScannerRegistryImpl(BundleContext bundleContext, RegistrationService registrationService) {
        super(bundleContext, ScannerManager.class, PLUGIN_IDENTIFIER, registrationService);
    }

    @Override
    public Map<String, ScannerManager> getServices() {
        return super.asMap();
    }
}
