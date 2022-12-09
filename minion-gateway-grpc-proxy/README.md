
| Setting Name | Default | Description |
| ------------ | ------- | ----------- |
| grpc.listen.port | 8990 | PORT on which to listen for incoming GRPC connections |
| grpc.inbound.max-message-size | | Max size of messages to accept on incoming GRPC connections |
| grpc.inbound.tls-enabled | false | Enable TLS on the inbound GRPC connections |
| grpc.downstream.host | | Name of the downstream host to which to proxy GRPC connections |
| grpc.downstream.port | | Port of the downstream to which to proxy GRPC connections |
| grpc.downstream.max-message-size | | Maximimum size of messages allowed on downstream GRPC connections |
| grpc.downstream.tls-enabled | false | Enable TLS on the downstream GRPC connections |
| grpc.downstream.inject-header-name | tenant-id | Name of the header to inject into GRPC communciations before forwarding downstream |
| grpc.downstream.inject-header-value | opennms-prime | Value of the header to inject into GRPC communciations before forwarding downstream |
