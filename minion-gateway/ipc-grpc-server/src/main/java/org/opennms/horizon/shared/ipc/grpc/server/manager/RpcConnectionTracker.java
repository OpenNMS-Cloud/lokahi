package org.opennms.horizon.shared.ipc.grpc.server.manager;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.Semaphore;
import org.opennms.cloud.grpc.minion.RpcRequestProto;

public interface RpcConnectionTracker {
    boolean addConnection(String location, String  minionId, StreamObserver<RpcRequestProto> connection);
    StreamObserver<RpcRequestProto> lookupByMinionId(String minionId);
    StreamObserver<RpcRequestProto> lookupByLocationRoundRobin(String locationId);
    MinionInfo removeConnection(StreamObserver<RpcRequestProto> connection);
    Semaphore getConnectionSemaphore(StreamObserver<RpcRequestProto> connection);

    void clear();
}
