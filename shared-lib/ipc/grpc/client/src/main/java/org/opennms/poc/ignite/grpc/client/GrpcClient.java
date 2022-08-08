package org.opennms.poc.ignite.grpc.client;

import io.grpc.ConnectivityState;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface GrpcClient extends AutoCloseable {

  // CloudToMinionRPC, async
  Session onCall(Predicate<RpcRequestProto> request, Function<RpcRequestProto, CompletableFuture<RpcResponseProto>> response);
  // CloudToMinionMessages, async
  Session streamFromCloud(Consumer<CloudToMinionMessage> consumer);

  // MinionToCloudRPC, blocking
  RpcResponseProto request(RpcRequestProto request);
  // MinionToCloudMessages
  PublishSession publishToCloud();

  void start();

  ConnectivityState getState();

  interface Session extends Closeable { }

  interface PublishSession extends Session {
    void publish(MinionToCloudMessage message);
  }

}
