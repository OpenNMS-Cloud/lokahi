package org.opennms.poc.ignite.grpc.client.internal;

import com.google.protobuf.Empty;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceBlockingStub;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceStub;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.MinionHeader;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.poc.ignite.grpc.client.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StubGrpcClient implements GrpcClient {

  private final Logger logger = LoggerFactory.getLogger(StubGrpcClient.class);

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "grpc-client-call"));
  private final ManagedChannel channel;
  private final CloudServiceStub asnc;
  private final CloudServiceBlockingStub blocking;
  private final Map<Predicate<RpcRequestProto>, Function<RpcRequestProto, CompletableFuture<RpcResponseProto>>> handlers = new ConcurrentHashMap<>();
  private final String minionServerId;
  private final String minionLocation;
  private StreamObserver<RpcResponseProto> rpcReplyStream;

  public StubGrpcClient(ManagedChannel channel, CloudServiceStub async, CloudServiceBlockingStub blocking, String minionServerId, String minionLocation) {
    this.channel = channel;
    this.asnc = async;
    this.blocking = blocking;
    this.minionServerId = minionServerId;
    this.minionLocation = minionLocation;
  }

  @Override
  public void start() {
    channel.getState(true);

    this.rpcReplyStream = asnc.cloudToMinionRPC(new StreamObserver<>() {
      @Override
      public void onNext(RpcRequestProto value) {
        handleIncomingRpc(value);
      }

      @Override
      public void onError(Throwable t) {

      }

      @Override
      public void onCompleted() {

      }
    });
    RpcResponseProto headers = RpcResponseProto.newBuilder()
      .setRpcId(UUID.randomUUID().toString())
      .setModuleId("MINION_HEADERS")
      .setSystemId(minionServerId)
      .setLocation(minionLocation)
      .build();
    rpcReplyStream.onNext(headers);
  }

  @Override
  public void close() throws Exception {
    if (getState() != ConnectivityState.SHUTDOWN) {
      channel.shutdown();
    }
  }

  @Override
  public ConnectivityState getState() {
    return channel.getState(false);
  }

  public Session onCall(Predicate<RpcRequestProto> predicate, Function<RpcRequestProto, CompletableFuture<RpcResponseProto>> function) {
    this.handlers.put(predicate, function);
    return new Session() {
      @Override
      public void close() throws IOException {
        handlers.remove(predicate);
      }
    };
  }

  public PublishSession publishToCloud() {
    StreamObserver<MinionToCloudMessage> observer = asnc.minionToCloudMessages(new EmptyObserver());
    return new PublishSession() {
      @Override
      public void close() throws IOException {
        observer.onCompleted();
      }

      @Override
      public void publish(MinionToCloudMessage message) {
        observer.onNext(message);
      }
    };
  }

  public Session streamFromCloud(Consumer<CloudToMinionMessage> consumer) {
    MinionHeader header = MinionHeader.newBuilder()
      .setLocation(minionLocation)
      .setSystemId(minionServerId)
      .build();

    StreamObserver<CloudToMinionMessage> observer = new ForwardingObserver<>(new Consumer<>() {
        @Override
        public void accept(CloudToMinionMessage cloudToMinionMessage) {
          consumer.accept(cloudToMinionMessage);
        }
      });
    asnc.cloudToMinionMessages(header, observer);
    return new Session() {
      @Override
      public void close() throws IOException {
        // do we close it ?!
        observer.onCompleted();
      }
    };
  }

  @Override
  public RpcResponseProto request(RpcRequestProto request) {
    return blocking.minionToCloudRPC(request);
  }

  private void handleIncomingRpc(RpcRequestProto request) {
    for (Entry<Predicate<RpcRequestProto>, Function<RpcRequestProto, CompletableFuture<RpcResponseProto>>> entry : handlers.entrySet()) {
      if (entry.getKey().test(request)) {
        entry.getValue().apply(request).whenComplete((response, error) -> {
          if (error != null) {
            logger.warn("Client side error while handling rpc request {}", request, error);
            return;
          }
          rpcReplyStream.onNext(response);
        });
      }
    }
  }

  static class EmptyObserver implements StreamObserver<Empty> {

    @Override
    public void onNext(Empty value) {

    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onCompleted() {

    }
  }

}
