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

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.minion.plugin.api.RegistrationService;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.osgi.framework.BundleContext;

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
