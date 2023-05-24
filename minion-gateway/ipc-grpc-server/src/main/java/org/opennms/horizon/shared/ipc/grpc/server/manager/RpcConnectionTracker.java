package org.opennms.horizon.shared.ipc.grpc.server.manager;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.Semaphore;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface RpcConnectionTracker {
    boolean addConnection(String tenantId, String location, String minionId, StreamObserver<RpcRequestProto> connection);
    StreamObserver<RpcRequestProto> lookupByMinionId(String tenantId, String minionId);
    StreamObserver<RpcRequestProto> lookupByLocationRoundRobin(String tenantId, String locationId);
    MinionInfo removeConnection(StreamObserver<RpcRequestProto> connection);
    Semaphore getConnectionSemaphore(StreamObserver<RpcRequestProto> connection);

    void clear();
}
