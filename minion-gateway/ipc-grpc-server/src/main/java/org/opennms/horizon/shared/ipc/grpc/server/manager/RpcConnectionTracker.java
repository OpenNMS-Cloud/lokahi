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
package org.opennms.horizon.shared.ipc.grpc.server.manager;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import java.util.concurrent.Semaphore;
import org.opennms.cloud.grpc.minion.RpcRequestProto;

public interface RpcConnectionTracker {
    boolean addConnection(
            String tenantId, String location, String minionId, StreamObserver<RpcRequestProto> connection);

    StreamObserver<RpcRequestProto> lookupByMinionId(String tenantId, String minionId);

    StreamObserver<RpcRequestProto> lookupByLocationRoundRobin(String tenantId, String locationId);

    MinionInfo removeConnection(StreamObserver<RpcRequestProto> connection);

    Semaphore getConnectionSemaphore(StreamObserver<RpcRequestProto> connection);

    SpanContext getConnectionSpanContext(StreamObserver<RpcRequestProto> connection);

    Attributes getConnectionSpanAttributes(StreamObserver<RpcRequestProto> connection);

    void clear();
}
