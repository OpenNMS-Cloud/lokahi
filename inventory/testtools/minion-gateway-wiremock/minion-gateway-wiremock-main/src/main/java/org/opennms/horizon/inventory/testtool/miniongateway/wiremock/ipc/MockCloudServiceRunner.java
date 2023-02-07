package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.ipc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

@Component
public class MockCloudServiceRunner {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MockCloudServiceRunner.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    private MockCloudServiceConfiguration configuration;

    @Autowired
    private List<BindableService> bindableServiceList;

    private NettyServerBuilder nettyServerBuilder;

    @PostConstruct
    public void start() {
        log.info("STARTING mock cloud service at port {} with max-message-size={}",
            configuration.getPort(),
            configuration.getMaxMessageSize());

        nettyServerBuilder =
            NettyServerBuilder.forAddress(new InetSocketAddress(configuration.getPort()))
                .maxInboundMessageSize(configuration.getMaxMessageSize())
                .addService(ProtoReflectionService.newInstance())
        ;

        if (bindableServiceList != null) {
            log.info("REGISTERING {} GRPC services", bindableServiceList.size());
            bindableServiceList.forEach(nettyServerBuilder::addService);
        } else {
            log.info("HAVE 0 GRPC services");
        }

        Server server = nettyServerBuilder.build();
        try {
            server.start();
        } catch (IOException ioException) {
            throw new RuntimeException("failed to start GRPC server", ioException);
        }
    }
}
