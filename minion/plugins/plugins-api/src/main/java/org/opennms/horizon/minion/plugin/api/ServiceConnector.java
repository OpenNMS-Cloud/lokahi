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

/**
 * Interface for a ServiceConnector, which is a long-running operation that creates a long-lived connection to a remote
 * endpoint and consumes a stream of samples.
 *
 * NOTE: plugins register the ServiceConnectorFactory with the ServiceConnectorFactoryRegistry.
 */
public interface ServiceConnector {
    /**
     * Attempt to create a connection to the remote.  Implementations may use their own disconnect/reconnect logic,
     * but it's not required - the Minion will automatically attempt to reconnect after failed connections / disconnects.
     *
     * @throws Exception
     */
    void connect() throws Exception;

    /**
     * Shutdown the active connection.
     */
    void disconnect();
}
