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
package org.opennms.horizon.minion.taskset.worker.impl;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import java.util.Collections;
import java.util.Map;
import org.opennms.horizon.minion.taskset.worker.TaskSetLifecycleManager;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.taskset.contract.TaskSet;

public class MeteredWorkflowLifecycleManager implements TaskSetLifecycleManager, MetricSet {

    private final IpcIdentity identity;
    private final TaskSetLifecycleManager delegate;
    private final Counter counter = new Counter();

    public MeteredWorkflowLifecycleManager(IpcIdentity identity, TaskSetLifecycleManager delegate) {
        this.identity = identity;
        this.delegate = delegate;
    }

    @Override
    public int deploy(TaskSet taskSet) {
        int size = taskSet.getTaskDefinitionList().size();
        int deployed = delegate.deploy(taskSet);
        counter.inc(size - deployed);
        return deployed;
    }

    @Override
    public TaskSet getDeployedTaskSet() {
        return delegate.getDeployedTaskSet();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.singletonMap(name("minion", identity.getId(), "taskset"), counter);
    }
}
