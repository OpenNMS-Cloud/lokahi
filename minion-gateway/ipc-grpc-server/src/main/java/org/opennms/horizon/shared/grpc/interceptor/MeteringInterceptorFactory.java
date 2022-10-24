package org.opennms.horizon.shared.grpc.interceptor;

import com.codahale.metrics.MetricRegistry;
import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

public class MeteringInterceptorFactory implements InterceptorFactory {

  private final MetricRegistry metricRegistry;

  public MeteringInterceptorFactory(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  @Override
  public BindableService create(BindableService service) {
    ServerServiceDefinition definition = ServerInterceptors.intercept(
      ServerInterceptors.useInputStreamMessages(service.bindService()), new MeteringServerInterceptor(metricRegistry)
    );
    return new BindableService() {
      @Override
      public ServerServiceDefinition bindService() {
        return definition;
      }
    };
  }
}
