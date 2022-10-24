package org.opennms.horizon.shared.grpc.interceptor;

import io.grpc.BindableService;

public interface InterceptorFactory {

  BindableService create(BindableService service);

}
