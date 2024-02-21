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
package org.opennms.horizon.shared.ipc.sink.common;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.protobuf.Message;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The state and metrics pertaining to a particular dispatches.
 *
 * @author jwhite
 */
public class DispatcherState<W, S extends Message, T extends Message> implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherState.class);

    private final SinkModule<S, T> module;

    private final W metadata;

    private final MetricRegistry metrics;

    private final Timer dispatchTimer;

    private final Counter dispatchCounter;

    public DispatcherState(AbstractMessageDispatcherFactory<W> dispatcherFactory, SinkModule<S, T> module) {
        this.module = module;
        metadata = dispatcherFactory.getModuleMetadata(module);
        metrics = dispatcherFactory.getMetrics();

        this.dispatchTimer = metrics.timer(MetricRegistry.name(module.getId(), "dispatch", "time"));
        this.dispatchCounter = metrics.counter(MetricRegistry.name(module.getId(), "dispatch", "count"));
    }

    public SinkModule<S, T> getModule() {
        return module;
    }

    public W getMetaData() {
        return metadata;
    }

    protected MetricRegistry getMetrics() {
        return metrics;
    }

    public Timer getDispatchTimer() {
        return dispatchTimer;
    }

    public Counter getDispatchCounter() {
        return this.dispatchCounter;
    }

    @Override
    public void close() throws Exception {
        final String prefix = MetricRegistry.name(module.getId());
        metrics.removeMatching(new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric) {
                return name.startsWith(prefix);
            }
        });
    }
}
