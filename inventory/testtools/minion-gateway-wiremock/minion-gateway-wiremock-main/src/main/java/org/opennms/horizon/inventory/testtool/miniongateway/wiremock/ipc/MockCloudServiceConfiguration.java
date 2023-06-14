package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.ipc;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockCloudServiceConfiguration {
    @Value("${mock.cloud-service.hostname:0.0.0.0}")
    @Getter
    private String hostname;

    // 8991 = "internal" (i.e. cloud-facing) GRPC port for Minion Gateway
    @Value("${mock.cloud-service.port:8991}")
    @Getter
    private int port;

    @Value("${mock.cloud-service.maxMessageSize:10485760}")
    @Getter
    private int maxMessageSize;
}
