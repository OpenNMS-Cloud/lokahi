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
package org.opennms.horizon.shared.snmp.snmp4j;

import java.util.Dictionary;
import java.util.Hashtable;
import org.opennms.horizon.shared.snmp.SnmpStrategy;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Snmp4JActivator implements BundleActivator {

    private ServiceRegistration<?> serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, String> props = new Hashtable<>();
        props.put("implementation", Snmp4JStrategy.class.getName());
        Snmp4JStrategy strategy = new Snmp4JStrategy();
        serviceRegistration = context.registerService(SnmpStrategy.class.getName(), strategy, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }
}
