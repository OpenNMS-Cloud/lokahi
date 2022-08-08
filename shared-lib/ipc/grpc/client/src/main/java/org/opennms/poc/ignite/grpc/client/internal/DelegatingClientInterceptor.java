package org.opennms.poc.ignite.grpc.client.internal;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports dynamic updates to the list of interceptors without restarting the GRPC client.
 */
public class DelegatingClientInterceptor implements ClientInterceptor {

  private final Logger logger = LoggerFactory.getLogger(DelegatingClientInterceptor.class);
  private final List<ClientInterceptor> interceptors;

  public DelegatingClientInterceptor(List<ClientInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
    ClientCall<ReqT, RespT> call = channel.newCall(methodDescriptor, callOptions);

    for (ClientInterceptor interceptor : interceptors) {
      interceptor.interceptCall(methodDescriptor, callOptions, new Channel() {
        @Override
        public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
          return (ClientCall<RequestT, ResponseT>) call;
        }

        @Override
        public String authority() {
          return channel.authority();
        }
      });
    }

    return call;
  }

  public static class GrpcClientRequestInterceptor implements ClientInterceptor {
    private final Logger logger = LoggerFactory.getLogger(GrpcClientRequestInterceptor.class);

    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
      return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(methodDescriptor, callOptions)) {
        @Override
        public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
          logger.info("Starting request with headers {}", headers);
          super.start(responseListener, headers);
        }
        @Override
        public void sendMessage(ReqT message) {
          logger.info("Starting request {}", message);
          super.sendMessage(message);
        }
      };
    }
  }

  public static class GrpcClientResponseInterceptor implements ClientInterceptor {
    private final Logger logger = LoggerFactory.getLogger(GrpcClientResponseInterceptor.class);

    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
      return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(methodDescriptor, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          SimpleForwardingClientCallListener<RespT> listener = new SimpleForwardingClientCallListener<>(responseListener) {
            @Override
            public void onMessage(RespT message) {
              logger.debug("Received response from server: {}, headers: {}", message, headers);
              super.onMessage(message);
            }
          };
          super.start(listener, headers);
        }

        @Override
        public void sendMessage(ReqT message) {
          logger.info("Sending request {}", message);
          super.sendMessage(message);
        }
      };
    }
  }
}