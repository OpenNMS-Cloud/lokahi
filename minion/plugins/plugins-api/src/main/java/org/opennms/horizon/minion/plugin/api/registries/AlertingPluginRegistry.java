package org.opennms.horizon.minion.plugin.api.registries;

import com.savoirtech.eos.pattern.whiteboard.KeyedWhiteboard;
import com.savoirtech.eos.util.ServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.PluginMetadata;
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.opennms.taskset.contract.TaskType;
import org.osgi.framework.BundleContext;

@Slf4j
public class AlertingPluginRegistry<K, S> extends KeyedWhiteboard<K, S>  {
    private final RegistrationService registrationService;

    public AlertingPluginRegistry(BundleContext bundleContext, Class<S> serviceType, String id, RegistrationService alertingService) {
        super(bundleContext, serviceType, (svc, props) -> props.getProperty(id));
        this.registrationService = alertingService;
        super.start();
    }

    @Override
    protected K addService(S service, ServiceProperties props) {
        K serviceId = super.addService(service, props);

        if (serviceId != null) {
            log.info("Performing scan on service {}", service.getClass());
            PluginMetadata pluginMetadata = new PluginMetadata(serviceId.toString(), TaskType.DETECTOR);
            if (registrationService != null) {
                registrationService.notifyOfPluginRegistration(pluginMetadata);
            }
        }

        return serviceId;
    }

    @Override
    public void start() {

    }
}
