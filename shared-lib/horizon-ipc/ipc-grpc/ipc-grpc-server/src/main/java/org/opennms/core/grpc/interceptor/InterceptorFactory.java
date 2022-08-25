package org.opennms.core.grpc.interceptor;

import io.grpc.BindableService;

public interface InterceptorFactory {

  BindableService create(BindableService service);

}
