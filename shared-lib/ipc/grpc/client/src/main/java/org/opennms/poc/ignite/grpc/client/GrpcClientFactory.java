package org.opennms.poc.ignite.grpc.client;

import io.grpc.ClientInterceptor;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public interface GrpcClientFactory {

  int DEFAULT_TWIN_GRPC_PORT = 8080;
  int DEFAULT_MESSAGE_SIZE = 10485760;

  default GrpcClient create(String host, int port) throws Exception {
    return create(host, port, Collections.emptyList());
  }

  GrpcClient create(String host, int port, List<ClientInterceptor> interceptors) throws Exception;

}
