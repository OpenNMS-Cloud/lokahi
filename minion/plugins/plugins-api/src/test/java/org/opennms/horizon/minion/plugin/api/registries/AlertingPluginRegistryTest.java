package org.opennms.horizon.minion.plugin.api.registries;

import com.savoirtech.eos.util.ServiceProperties;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opennms.horizon.minion.plugin.api.PluginMetadata;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResults;
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.opennms.taskset.contract.TaskType;
import org.osgi.framework.BundleContext;

public class AlertingPluginRegistryTest {

    private static final String TEST_ID = "test.id";
    
    @Mock
    RegistrationService registrationService;
    @Mock
    BundleContext bundleContext;
    @Mock
    ServiceProperties serviceProperties;

    TestRegistry testRegistry;

    @Captor
    ArgumentCaptor<PluginMetadata> pluginMetadataCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        Mockito.when(serviceProperties.getServiceId()).thenReturn(10L);
        Mockito.when(serviceProperties.getProperty(ArgumentMatchers.anyString())).thenReturn(TEST_ID);
        Mockito.doNothing().when(registrationService).notifyOfPluginRegistration(pluginMetadataCaptor.capture());

        testRegistry = new TestRegistry(bundleContext,"blah", registrationService);
    }

    @Test
    public void addService() {

        testRegistry.addService(new TestPlugin(), serviceProperties);

        Mockito.verify(registrationService).notifyOfPluginRegistration(ArgumentMatchers.any(PluginMetadata.class));
        PluginMetadata pluginMetadata = pluginMetadataCaptor.getValue();
        Assert.assertNotNull(pluginMetadata);
        Assert.assertEquals(TEST_ID, pluginMetadata.getPluginName());
        Assert.assertEquals(TaskType.DETECTOR, pluginMetadata.getPluginType());
        //assertEquals(1, pluginMetadata.getFieldConfigs().size());
    }

    private class TestPlugin implements ServiceDetectorManager {

        @Override
        public ServiceDetector create(Consumer<ServiceDetectorResults> resultProcessor) {
            return null;
        }
    }

    private class  TestRegistry extends AlertingPluginRegistry<String, ServiceDetectorManager> {

        public TestRegistry(BundleContext bundleContext, String id, RegistrationService alertingService) {
            super(bundleContext, ServiceDetectorManager.class, id, alertingService);
        }
    }
}
