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
package org.opennms.horizon.shared.ipc.grpc.server.manager.adapter;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.trace.Span;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceImplBase;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageHandler;

public class MinionRSTransportAdapter extends CloudServiceImplBase {

    private final Function<StreamObserver<RpcRequestProto>, StreamObserver<RpcResponseProto>> cloudToMinionRPC;
    private final OutgoingMessageHandler cloudToMinionMessages;
    private final BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> minionToCloudRPC;
    private final Function<StreamObserver<Empty>, StreamObserver<MinionToCloudMessage>> minionToCloudMessages;

    public MinionRSTransportAdapter(
            Function<StreamObserver<RpcRequestProto>, StreamObserver<RpcResponseProto>> cloudToMinionRPC,
            OutgoingMessageHandler cloudToMinionMessages,
            BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> minionToCloudRPC,
            Function<StreamObserver<Empty>, StreamObserver<MinionToCloudMessage>> minionToCloudMessages) {
        this.cloudToMinionRPC = cloudToMinionRPC;
        this.cloudToMinionMessages = cloudToMinionMessages;
        this.minionToCloudRPC = minionToCloudRPC;
        this.minionToCloudMessages = minionToCloudMessages;
    }

    @Override
    public StreamObserver<RpcResponseProto> cloudToMinionRPC(StreamObserver<RpcRequestProto> responseObserver) {
        return cloudToMinionRPC.apply(responseObserver);
    }

    @Override
    public void cloudToMinionMessages(Identity request, StreamObserver<CloudToMinionMessage> responseObserver) {
        cloudToMinionMessages.handleOutgoingStream(request, responseObserver, Span.current());
    }

    @Override
    public void minionToCloudRPC(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
        minionToCloudRPC.accept(request, responseObserver);
    }

    @Override
    public StreamObserver<MinionToCloudMessage> minionToCloudMessages(StreamObserver<Empty> responseObserver) {
        return minionToCloudMessages.apply(responseObserver);
    }
}
