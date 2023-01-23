package org.opennms.horizon.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.service.GrpcAzureCredentialService;
import org.opennms.horizon.server.service.GrpcEventService;
import org.opennms.horizon.server.service.GrpcLocationService;
import org.opennms.horizon.server.service.GrpcMinionService;
import org.opennms.horizon.server.service.GrpcNodeService;
import org.opennms.horizon.server.service.NotificationService;
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
    private GrpcAzureCredentialService grpcAzureCredentialService;

	@Test
	void contextLoads() {
        assertNotNull(grpcMinionService);
        assertNotNull(notificationService);
        assertNotNull(grpcLocationService);
        assertNotNull(grpcEventService);
        assertNotNull(grpcNodeService);
        assertNotNull(grpcAzureCredentialService);
	}

}
