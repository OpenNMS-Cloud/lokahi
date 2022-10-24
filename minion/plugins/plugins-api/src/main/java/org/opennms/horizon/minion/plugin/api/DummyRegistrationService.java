package org.opennms.horizon.minion.plugin.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//NOTE: this approach of gathering plugin metadata is still awaiting final requirements and design review.
// For now, this is a stub to satisfy wiring in blueprint

public class DummyRegistrationService implements RegistrationService {

    private final Logger logger = LoggerFactory.getLogger(DummyRegistrationService.class);

    @Override
    public void notifyOfPluginRegistration(PluginMetadata pluginMetadata) {
        logger.info("Detected plugin at runtime {}. Skipping its online registration for now.", pluginMetadata);
    }
}
