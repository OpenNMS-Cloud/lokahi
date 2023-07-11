package org.opennms.horizon.shared.grpc.interceptor;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.KnownLength;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.MeterRegistry;

import java.io.IOException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteringServerInterceptor implements ServerInterceptor {

  private final Logger logger = LoggerFactory.getLogger(MeteringServerInterceptor.class);
  private final MeterRegistry meterRegistry;

  public MeteringServerInterceptor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    final var counter = meterRegistry.counter("grpc.calls",
        "service", call.getMethodDescriptor().getServiceName(),
        "method", call.getMethodDescriptor().getBareMethodName());
    counter.increment();

    SimpleForwardingServerCall<ReqT, RespT> serverCall = new SimpleForwardingServerCall<>(call) {
      @Override
      public void sendMessage(RespT message) {
        if (message instanceof KnownLength) {
          try {
            int bytes = ((KnownLength) message).available();
            final var histogram = meterRegistry.summary("grpc.outgoing.size",
                "service", call.getMethodDescriptor().getServiceName(),
                "method", call.getMethodDescriptor().getBareMethodName());
            histogram.record(bytes);
          } catch (IOException e) {
            logger.warn("Error while obtaining payload length", e);
          }
        }

        final var counter = meterRegistry.counter("grpc.outgoing.count",
            "service", call.getMethodDescriptor().getServiceName(),
            "method", call.getMethodDescriptor().getBareMethodName());
        counter.increment();
        super.sendMessage(message);
      }
    };
    
    return new MeteredListener<>(meterRegistry, next.startCall(serverCall, headers), call.getMethodDescriptor());
  }

  static class MeteredListener<ReqT> extends Listener<ReqT> {

    private final MeterRegistry meterRegistry;
    private final Listener<ReqT> delegate;

    private final MethodDescriptor<?, ?> methodDescriptor;

    public MeteredListener(MeterRegistry meterRegistry, Listener<ReqT> delegate, MethodDescriptor<?, ?> methodDescriptor) {
      this.meterRegistry = meterRegistry;
      this.delegate = delegate;
      this.methodDescriptor = methodDescriptor;
    }

    @Override
    public void onMessage(ReqT message) {
      if (message instanceof KnownLength) {
        final var incoming = meterRegistry.summary("grpc.incoming.size",
            "service", methodDescriptor.getServiceName(),
            "method", methodDescriptor.getBareMethodName());

        try {
          incoming.record(((KnownLength) message).available());
        } catch (IOException e) {
          //logger.warn("");
        }
      }
      final var counter = meterRegistry.counter("grpc.incoming.count",
        "service", methodDescriptor.getServiceName(),
            "method", methodDescriptor.getBareMethodName());
      counter.increment();
      delegate.onMessage(message);
    }

    @Override
    public void onHalfClose() {
      delegate.onHalfClose();
    }

    @Override
    public void onCancel() {
      delegate.onCancel();
    }

    @Override
    public void onComplete() {
      delegate.onComplete();
    }

    @Override
    public void onReady() {
      delegate.onReady();
    }
  }
}
