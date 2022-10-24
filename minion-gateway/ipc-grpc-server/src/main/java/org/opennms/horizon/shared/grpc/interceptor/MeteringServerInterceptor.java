package org.opennms.horizon.shared.grpc.interceptor;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.KnownLength;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.io.IOException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteringServerInterceptor implements ServerInterceptor {

  private final Logger logger = LoggerFactory.getLogger(MeteringServerInterceptor.class);
  private final MetricRegistry metricRegistry;

  public MeteringServerInterceptor(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    String baseMetricName = name(call.getMethodDescriptor().getServiceName(), call.getMethodDescriptor().getBareMethodName());

    Counter counter = metric(metricRegistry, name(baseMetricName, "count"), Counter::new);
    counter.inc();
    logger.info("Call counter {}", counter.getCount());

    SimpleForwardingServerCall<ReqT, RespT> serverCall = new SimpleForwardingServerCall<>(call) {
      @Override
      public void sendMessage(RespT message) {
        if (message instanceof KnownLength) {
          try {
            int bytes = ((KnownLength) message).available();
            Histogram histogram = metric(metricRegistry, name(baseMetricName, "outgoing"), () -> new Histogram(new ExponentiallyDecayingReservoir()));
            histogram.update(bytes);
          } catch (IOException e) {
            logger.warn("Error while obtaining payload length", e);
          }
        }

        Counter counter = metric(metricRegistry, name(baseMetricName, "outgoing_message_count"), Counter::new);
        counter.inc();
        super.sendMessage(message);
      }
    };
    return new MeteredListener<>(metricRegistry, next.startCall(serverCall, headers), baseMetricName);
  }

  static <T extends Metric> T metric(MetricRegistry metricRegistry, String name, Supplier<T> fallback) {
    String metricName = name("grpc_server", name);
    if (!metricRegistry.getMetrics().containsKey(metricName)) {
      metricRegistry.register(metricName, fallback.get());
    }
    return (T) metricRegistry.getMetrics().get(metricName);
  }

  static class MeteredListener<ReqT> extends Listener<ReqT> {

    private final MetricRegistry metricRegistry;
    private final Listener<ReqT> delegate;
    private final String baseMetricName;

    public MeteredListener(MetricRegistry metricRegistry, Listener<ReqT> delegate, String baseMetricName) {
      this.metricRegistry = metricRegistry;
      this.delegate = delegate;
      this.baseMetricName = baseMetricName;
    }

    @Override
    public void onMessage(ReqT message) {
      if (message instanceof KnownLength) {
        Histogram incoming = metric(metricRegistry, name(baseMetricName, "incoming"), () -> new Histogram(new ExponentiallyDecayingReservoir()));
        try {
          incoming.update(((KnownLength) message).available());
        } catch (IOException e) {
          //logger.warn("");
        }
      }
      Counter counter = metric(metricRegistry, name(baseMetricName, "incoming_message_count"), Counter::new);
      counter.inc();
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
