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

import com.savoirtech.eos.pattern.whiteboard.KeyedWhiteboard;
import com.savoirtech.eos.util.ServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.PluginMetadata;
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.opennms.taskset.contract.TaskType;
import org.osgi.framework.BundleContext;

@Slf4j
public class AlertingPluginRegistry<K, S> extends KeyedWhiteboard<K, S> {
    private final RegistrationService registrationService;

    public AlertingPluginRegistry(
            BundleContext bundleContext, Class<S> serviceType, String id, RegistrationService alertingService) {
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
    public void start() {}
}
