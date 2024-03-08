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
package org.opennms.horizon.shared.ipc.rpc.api;

import com.google.protobuf.Message;
import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;

/**
 * Asynchronously executes remote procedure calls (RPCs).
 *
 * @author jwhite
 */
public interface RpcClient<T extends Message> {

    /**
     *
     * @param tenantId the Tenant to which the request applies.  The tenant is not in the request payload itself because
     *                 the request gets sent to the minions, and we never explicitly send the tenant ID to services
     *                 running on-site.
     * @param request
     * @return
     */
    CompletableFuture<T> execute(String tenantId, RpcRequestProto request);

    RequestBuilder builder(String module);
}
