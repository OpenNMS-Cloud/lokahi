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
package org.opennms.horizon.minion.observability.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.savoirtech.eos.pattern.whiteboard.AbstractWhiteboard;
import com.savoirtech.eos.util.ServiceProperties;
import org.osgi.framework.BundleContext;

public class MetricsWhiteboard extends AbstractWhiteboard<MetricSet, MetricSet> {

    private final MetricRegistry metricRegistry;

    public MetricsWhiteboard(BundleContext bundleContext) {
        this(new MetricRegistry(), bundleContext);
    }

    public MetricsWhiteboard(MetricRegistry metricRegistry, BundleContext bundleContext) {
        super(bundleContext, MetricSet.class);
        this.metricRegistry = metricRegistry;
    }

    @Override
    protected MetricSet addService(MetricSet service, ServiceProperties props) {
        metricRegistry.registerAll(service);
        return service;
    }

    @Override
    protected void removeService(MetricSet service, MetricSet tracked) {
        for (String metric : service.getMetrics().keySet()) {
            metricRegistry.remove(metric);
        }
    }
}
