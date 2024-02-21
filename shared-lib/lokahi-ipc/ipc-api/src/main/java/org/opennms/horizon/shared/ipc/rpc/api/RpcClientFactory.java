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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

/**
 * Creates a {@link RpcClient} that can be used to invoke RPCs against the given {@link RpcModule}.
 *
 * @author jwhite
 */
public interface RpcClientFactory {
    String LOG_PREFIX = "ipc";

    <T extends Message> RpcClient<T> getClient(Deserializer<T> deserializer);

    RpcClient<RpcResponseProto> getClient();

    interface Deserializer<T extends Message> {
        T deserialize(RpcResponseProto response) throws InvalidProtocolBufferException;
    }
}
