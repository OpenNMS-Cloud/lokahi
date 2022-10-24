package org.opennms.horizon.shared.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingInterceptor implements ServerInterceptor {

  private final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    logger.trace("Received call {} with headers {}", call, headers);
    return next.startCall(call, headers);
  }
}
