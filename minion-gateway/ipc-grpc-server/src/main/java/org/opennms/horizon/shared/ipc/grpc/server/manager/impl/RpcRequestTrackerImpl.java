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
package org.opennms.horizon.shared.ipc.grpc.server.manager.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;

public class RpcRequestTrackerImpl implements RpcRequestTracker {

    private final Map<String, RpcResponseHandler> requestMap = new ConcurrentHashMap<>();

    @Override
    public void addRequest(String id, RpcResponseHandler responseHandler) {
        this.requestMap.put(id, responseHandler);
    }

    @Override
    public RpcResponseHandler lookup(String id) {
        return this.requestMap.get(id);
    }

    @Override
    public void remove(String id) {
        this.requestMap.remove(id);
    }

    @Override
    public void clear() {
        this.requestMap.clear();
    }
}
