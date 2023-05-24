package org.opennms.horizon.testtool.miniongateway.wiremock.ipc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockGrpcServiceApi;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class MockCloudService extends CloudServiceGrpc.CloudServiceImplBase implements MockGrpcServiceApi {

    private final List<StreamObserver<CloudToMinionMessage>> cloudToLocationStreamMap = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger();
    private final Map<String, StreamObserver<CloudToMinionMessage>> cloudToMinionStreamMap = new ConcurrentHashMap<>();

    private final Set<Identity> connectedMinions = Collections.synchronizedSet(new HashSet<>());
    @Getter
    private final List<SinkMessage> receivedSinkMessages = new CopyOnWriteArrayList<>();

//========================================
// INTERFACE
//----------------------------------------

    @Override
    public void sendMessageToLocation(CloudToMinionMessage message) {
        var streamObserverList = cloudToLocationStreamMap;
        int seq = roundRobinCounter.getAndIncrement();

        var streamObserver= streamObserverList.get(seq % streamObserverList.size());
        streamObserver.onNext(message);
    }

    @Override
    public void sendMessageToMinion(String minionId, CloudToMinionMessage message) {
        StreamObserver<CloudToMinionMessage> streamObserver = cloudToMinionStreamMap.get(minionId);
        if (streamObserver != null) {
            streamObserver.onNext(message);
        } else {
            throw new RuntimeException("don't have a connection for Minion " + minionId);
        }
    }

    @Override
    public List<Identity> getConnectedMinions() {
        return new LinkedList<>(connectedMinions);
    }

//========================================
// GRPC Service Endpoints
//----------------------------------------

    @Override
    public void cloudToMinionMessages(Identity minionIdentity, StreamObserver<CloudToMinionMessage> streamObserver) {
        String systemId = minionIdentity.getSystemId();
        log.info("Have cloud-to-minion stream connection request: system-id={}", systemId);

        connectedMinions.add(minionIdentity);
        cloudToMinionStreamMap.put(systemId, streamObserver);
        cloudToLocationStreamMap.add(streamObserver);
    }

    @Override
    public StreamObserver<MinionToCloudMessage> minionToCloudMessages(StreamObserver<Empty> responseObserver) {
        log.info("Have minion-to-cloud message initiated");
        return new StreamObserver<>() {
            @Override
            public void onNext(MinionToCloudMessage value) {
                log.info("Have minion-to-cloud-message from module {}: twin-request.consumer-key={}",
                    value.getSinkMessage().getModuleId(),
                    value.getTwinRequest().getConsumerKey());
                receivedSinkMessages.add(value.getSinkMessage());
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Have minion-to-cloud-message Exception.", t);
            }

            @Override
            public void onCompleted() {

            }
        };
    }

    public StreamObserver<RpcResponseProto> cloudToMinionRPC(StreamObserver<RpcRequestProto> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(RpcResponseProto value) {
                log.info("cloudToMinionRPC called.");
            }

            @Override
            public void onError(Throwable t) {
                log.warn("cloudToMinionRPC Exception.", t);
            }

            @Override
            public void onCompleted() {

            }
        };
    }
}
