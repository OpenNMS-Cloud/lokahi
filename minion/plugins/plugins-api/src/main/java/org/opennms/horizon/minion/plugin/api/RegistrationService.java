package org.opennms.horizon.minion.plugin.api;

public interface RegistrationService {
    void notifyOfPluginRegistration(PluginMetadata pluginMetadataJson);
}
