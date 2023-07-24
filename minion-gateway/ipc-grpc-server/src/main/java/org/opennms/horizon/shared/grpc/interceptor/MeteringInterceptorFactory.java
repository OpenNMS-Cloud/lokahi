package org.opennms.horizon.shared.grpc.interceptor;

import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.micrometer.core.instrument.MeterRegistry;

public class MeteringInterceptorFactory implements InterceptorFactory {

  private final MeterRegistry meterRegistry;

  public MeteringInterceptorFactory(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public BindableService create(BindableService service) {
    ServerServiceDefinition definition = ServerInterceptors.intercept(
        ServerInterceptors.useInputStreamMessages(service.bindService()),
        new MeteringServerInterceptor(this.meterRegistry)
    );
    return new BindableService() {
      @Override
      public ServerServiceDefinition bindService() {
        return definition;
      }
    };
  }
}
