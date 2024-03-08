/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.plugin.api.registries;

import com.savoirtech.eos.util.ServiceProperties;
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
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
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
        Mockito.when(serviceProperties.getProperty(ArgumentMatchers.anyString()))
                .thenReturn(TEST_ID);
        Mockito.doNothing().when(registrationService).notifyOfPluginRegistration(pluginMetadataCaptor.capture());

        testRegistry = new TestRegistry(bundleContext, "blah", registrationService);
    }

    @Test
    public void addService() {

        testRegistry.addService(new TestPlugin(), serviceProperties);

        Mockito.verify(registrationService).notifyOfPluginRegistration(ArgumentMatchers.any(PluginMetadata.class));
        PluginMetadata pluginMetadata = pluginMetadataCaptor.getValue();
        Assert.assertNotNull(pluginMetadata);
        Assert.assertEquals(TEST_ID, pluginMetadata.getPluginName());
        Assert.assertEquals(TaskType.DETECTOR, pluginMetadata.getPluginType());
        // assertEquals(1, pluginMetadata.getFieldConfigs().size());
    }

    private class TestPlugin implements ServiceDetectorManager {

        @Override
        public ServiceDetector create() {
            return null;
        }
    }

    private class TestRegistry extends AlertingPluginRegistry<String, ServiceDetectorManager> {

        public TestRegistry(BundleContext bundleContext, String id, RegistrationService alertingService) {
            super(bundleContext, ServiceDetectorManager.class, id, alertingService);
        }
    }
}
