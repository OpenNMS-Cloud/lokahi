package org.opennms.horizon.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.service.GrpcAlertService;
import org.opennms.horizon.server.service.GrpcEventService;
import org.opennms.horizon.server.service.GrpcLocationService;
import org.opennms.horizon.server.service.GrpcMinionService;
import org.opennms.horizon.server.service.GrpcNodeService;
import org.opennms.horizon.server.service.NotificationService;
import org.opennms.horizon.server.service.discovery.GrpcAzureActiveDiscoveryService;
import org.opennms.horizon.server.service.flows.GrpcFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RestServerApplicationTests {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private GrpcMinionService grpcMinionService;
    @Autowired
    private GrpcEventService grpcEventService;
    @Autowired
    private GrpcNodeService grpcNodeService;
    @Autowired
    private GrpcLocationService grpcLocationService;
    @Autowired
    private GrpcAlertService grpcAlertService;
    @Autowired
    private GrpcAzureActiveDiscoveryService grpcAzureActiveDiscoveryService;
    @Autowired
    private GrpcFlowService grpcFlowService;

	@Test
	void contextLoads() {
        assertNotNull(grpcMinionService);
        assertNotNull(notificationService);
        assertNotNull(grpcLocationService);
        assertNotNull(grpcEventService);
        assertNotNull(grpcNodeService);
        assertNotNull(grpcAlertService);
        assertNotNull(grpcAzureActiveDiscoveryService);
        assertNotNull(grpcFlowService);
	}

}
