package org.opennms.horizon.shared.grpc.interceptor;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.KnownLength;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MeteringServerInterceptor implements ServerInterceptor {
  public static final String SERVICE_TAG_NAME = "service";
  public static final String METHOD_TAG_NAME =  "method";
  private final Logger logger = LoggerFactory.getLogger(MeteringServerInterceptor.class);
  private final MeterRegistry meterRegistry;

  public MeteringServerInterceptor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    final var counter = meterRegistry.counter("grpc.calls",
        SERVICE_TAG_NAME, call.getMethodDescriptor().getServiceName(),
        METHOD_TAG_NAME, call.getMethodDescriptor().getBareMethodName());
    counter.increment();

    SimpleForwardingServerCall<ReqT, RespT> serverCall = new SimpleForwardingServerCall<>(call) {
      @Override
      public void sendMessage(RespT message) {
        if (message instanceof KnownLength knownLength) {
          try {
            int bytes = knownLength.available();
            final var histogram = meterRegistry.summary("grpc.outgoing.size",
                SERVICE_TAG_NAME, call.getMethodDescriptor().getServiceName(),
                METHOD_TAG_NAME, call.getMethodDescriptor().getBareMethodName());
            histogram.record(bytes);
          } catch (IOException e) {
            logger.warn("Error while obtaining payload length", e);
          }
        }

        final var counter = meterRegistry.counter("grpc.outgoing.count",
            SERVICE_TAG_NAME, call.getMethodDescriptor().getServiceName(),
            METHOD_TAG_NAME, call.getMethodDescriptor().getBareMethodName());
        counter.increment();
        super.sendMessage(message);
      }
    };
    
    return new MeteredListener<>(meterRegistry, next.startCall(serverCall, headers), call.getMethodDescriptor());
  }

  static class MeteredListener<ReqT> extends Listener<ReqT> {
    private final Logger logger = LoggerFactory.getLogger(MeteredListener.class);

    private final Listener<ReqT> delegate;

    private final MethodDescriptor<?, ?> methodDescriptor;

    private final DistributionSummary incomingSummary;
    private final Counter incomingCounter;

    public MeteredListener(MeterRegistry meterRegistry, Listener<ReqT> delegate, MethodDescriptor<?, ?> methodDescriptor) {
      this.delegate = delegate;
      this.methodDescriptor = methodDescriptor;

      incomingSummary = meterRegistry.summary("grpc.incoming.size",
        SERVICE_TAG_NAME, methodDescriptor.getServiceName(),
        METHOD_TAG_NAME, methodDescriptor.getBareMethodName());
      incomingCounter = meterRegistry.counter("grpc.incoming.count",
        SERVICE_TAG_NAME, methodDescriptor.getServiceName(),
        METHOD_TAG_NAME, methodDescriptor.getBareMethodName());
    }

    @Override
    public void onMessage(ReqT message) {
      if (message instanceof KnownLength knownLength) {
        try {
            incomingSummary.record(knownLength.available());
        } catch (IOException e) {
            logger.warn("Fail to get message length.", e);
        }
      }

      incomingCounter.increment();
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
