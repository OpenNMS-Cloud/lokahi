package org.opennms.poc.ignite.grpc.client.internal;

import io.grpc.stub.StreamObserver;
import java.util.function.Consumer;

public class ForwardingObserver<T> implements StreamObserver<T> {

  private final Consumer<T> consumer;

  public ForwardingObserver(Consumer<T> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void onNext(T value) {
    consumer.accept(value);
  }

  @Override
  public void onError(Throwable t) {

  }

  @Override
  public void onCompleted() {

  }
}
