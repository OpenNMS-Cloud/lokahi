package org.opennms.horizon.shared.ipc.grpc.server.manager.adapter;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface CloudServiceDelegate {

  StreamObserver<RpcResponseProto> cloudToMinionRPC(StreamObserver<RpcRequestProto> responseObserver);

  void cloudToMinionMessages(Identity request, StreamObserver<CloudToMinionMessage> responseObserver);

  void minionToCloudRPC(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver);

  StreamObserver<MinionToCloudMessage> minionToCloudMessages(StreamObserver<Empty> responseObserver);
}
