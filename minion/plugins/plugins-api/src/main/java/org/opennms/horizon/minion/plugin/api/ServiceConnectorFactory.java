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
package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Any;
import java.util.function.Consumer;

/**
 * Interface for a ListenerFactory, which constructs Listeners.
 */
public interface ServiceConnectorFactory {
    /**
     * Create a new connector that will be used to connect to the remote.
     *
     * @param resultProcessor consumer of ServiceMonitorResponse results to be called as samples are received over the
     *                        connection
     * @param config configuration for the connector
     * @param disconnectHandler runnable executed on disconnect that enables the Minion to schedule reconnection
     *                          attempts; should only be called on disconnect after a successful connect attempt, and
     *                          not when the connect() method on the ServiceConnector throws an exception.
     * @return
     */
    ServiceConnector create(Consumer<ServiceMonitorResponse> resultProcessor, Any config, Runnable disconnectHandler);
}
