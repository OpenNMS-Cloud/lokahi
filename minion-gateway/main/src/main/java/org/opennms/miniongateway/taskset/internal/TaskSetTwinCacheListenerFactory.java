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
package org.opennms.miniongateway.taskset.internal;

import javax.cache.configuration.Factory;
import org.opennms.miniongateway.taskset.service.TaskSetStorageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that creates listeners for updates to TaskSets in the Ignite Cache and forwards the updates to Twin
 *  subscriptions.
 */
public class TaskSetTwinCacheListenerFactory implements Factory<TaskSetTwinCacheListener> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetTwinCacheListenerFactory.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final TaskSetStorageListener downstreamSession;

    public TaskSetTwinCacheListenerFactory(TaskSetStorageListener downstreamSession) {
        this.downstreamSession = downstreamSession;
    }

    @Override
    public TaskSetTwinCacheListener create() {
        LOG.debug("Creating listener for task set updates");
        return new TaskSetTwinCacheListener(downstreamSession);
    }
}
