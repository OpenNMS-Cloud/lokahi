package org.opennms.miniongatewaygrpcproxy.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.springframework.stereotype.Component;

@Component
public class GrpcHeaderCaptureInterceptor implements ServerInterceptor {

    public static final Context.Key<Metadata> INBOUND_HEADERS_CONTEXT_KEY = Context.key("inbound-headers");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        /**
         * Store the headers on the Context
         */
        Context context = Context.current().withValue(INBOUND_HEADERS_CONTEXT_KEY, headers);
        return Contexts.interceptCall(context, call, headers, next);
    }
}
