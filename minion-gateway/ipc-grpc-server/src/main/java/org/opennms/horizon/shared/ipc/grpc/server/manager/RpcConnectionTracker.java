package org.opennms.horizon.shared.ipc.grpc.server.manager;

import java.util.concurrent.Semaphore;

import org.opennms.cloud.grpc.minion.RpcRequestProto;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;

public interface RpcConnectionTracker {
    boolean addConnection(String tenantId, String location, String minionId, StreamObserver<RpcRequestProto> connection);
    StreamObserver<RpcRequestProto> lookupByMinionId(String tenantId, String minionId);
    StreamObserver<RpcRequestProto> lookupByLocationRoundRobin(String tenantId, String locationId);
    MinionInfo removeConnection(StreamObserver<RpcRequestProto> connection);
    Semaphore getConnectionSemaphore(StreamObserver<RpcRequestProto> connection);
    SpanContext getConnectionSpanContext(StreamObserver<RpcRequestProto> connection);
    Attributes getConnectionSpanAttributes(StreamObserver<RpcRequestProto> connection);

    void clear();
}
