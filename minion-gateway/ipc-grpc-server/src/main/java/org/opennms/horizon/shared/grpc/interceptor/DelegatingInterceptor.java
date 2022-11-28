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
      // Use the previous listener with the next interceptor call
      final Listener<ReqT> listenerAsFinal = listener;
      Listener<ReqT> nextListener =
          interceptor.interceptCall(call, headers, (call1, headers1) -> listenerAsFinal);

      // Update the listener to the new one returned by the interceptor - in this was, the listeners are (usually)
      //  chained by the interceptors.
      listener = nextListener;
    }

    // Return the listener returned by the last interceptor
    return listener;
  }
}
