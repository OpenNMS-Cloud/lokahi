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
import org.opennms.cloud.grpc.minion.RpcRequestProto;

/**
 * Builder which keeps care of null safety for constructed {@link RpcRequestProto} instances.
 *
 * Each call to {@link #build()} method will produce new instance of request with unique identifier.
 */
public interface RequestBuilder {

    RequestBuilder withExpirationTime(long ttl);

    RequestBuilder withLocation(String location);

    RequestBuilder withSystemId(String systemId);

    RequestBuilder withPayload(Message payload);

    RpcRequestProto build();
}
