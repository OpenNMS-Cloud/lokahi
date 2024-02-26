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
package org.opennms.horizon.minion.taskset.worker.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.processors.resource.GridSpringResourceContextImpl;
import org.opennms.horizon.minion.taskset.worker.ignite.resource.ApplicationContextAdapter;
import org.opennms.horizon.minion.taskset.worker.ignite.resource.BlueprintBeanRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

public class IgnitionFactory {
    public static Ignite create(
            WorkerIgniteConfiguration workerIgniteConfiguration,
            BundleContext bundleContext,
            BlueprintContainer container)
            throws Exception {
        return IgnitionEx.start(
                workerIgniteConfiguration.prepareIgniteConfiguration(),
                new GridSpringResourceContextImpl(new ApplicationContextAdapter(new BlueprintBeanRegistry(container))));
    }
}
