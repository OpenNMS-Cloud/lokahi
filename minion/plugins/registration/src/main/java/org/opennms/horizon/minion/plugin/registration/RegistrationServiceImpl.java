package org.opennms.horizon.minion.plugin.registration;

import lombok.AllArgsConstructor;
import org.apache.camel.ProducerTemplate;
import org.opennms.horizon.minion.plugin.api.PluginMetadata;
import org.opennms.horizon.minion.plugin.api.RegistrationService;

@AllArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final ProducerTemplate producerTemplate;

    @Override
    public void notifyOfPluginRegistration(PluginMetadata pluginMetadata) {
        producerTemplate.sendBody(pluginMetadata);
    }
}
