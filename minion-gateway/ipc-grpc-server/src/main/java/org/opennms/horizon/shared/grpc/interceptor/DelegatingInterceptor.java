package org.opennms.horizon.shared.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.List;

public class DelegatingInterceptor implements ServerInterceptor {

  private final List<ServerInterceptor> interceptors;

  public DelegatingInterceptor(List<ServerInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    Listener<ReqT> listener = next.startCall(call, headers);

    for (ServerInterceptor interceptor : interceptors) {
      interceptor.interceptCall(call, headers, new ServerCallHandler<ReqT, RespT>() {
        @Override
        public Listener<ReqT> startCall(ServerCall<ReqT, RespT> call, Metadata headers) {
          return listener;
        }
      });
    }

    return listener;
  }
}
