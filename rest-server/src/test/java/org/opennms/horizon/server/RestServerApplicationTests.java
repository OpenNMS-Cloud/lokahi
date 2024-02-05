package org.opennms.horizon.server;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.service.AlertGraphQLService;
import org.opennms.horizon.server.service.EventGraphQLService;
import org.opennms.horizon.server.service.GrpcMinionService;
import org.opennms.horizon.server.service.NodeGraphQLService;
import org.opennms.horizon.server.service.LocationGraphQLService;
import org.opennms.horizon.server.service.MinionGraphQLService;
import org.opennms.horizon.server.service.NotificationGraphQLService;
import org.opennms.horizon.server.service.discovery.AzureActiveDiscoveryGraphQLService;
import org.opennms.horizon.server.service.flows.FlowGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class RestServerApplicationTests {
    @Autowired
    private NotificationGraphQLService notificationGraphQLService;
    @Autowired
    private MinionGraphQLService grpcMinionService;
    @Autowired
    private EventGraphQLService eventGraphQLService;
    @Autowired
    private NodeGraphQLService grpcNodeService;
    @Autowired
    private LocationGraphQLService locationGraphQLService;
    @Autowired
    private AlertGraphQLService alertGraphQLService;
    @Autowired
    private AzureActiveDiscoveryGraphQLService azureActiveDiscoveryGraphQLService;
    @Autowired
    private FlowGraphQLService flowGraphQLService;

	@Test
	void contextLoads() {
        assertNotNull(grpcMinionService);
        assertNotNull(notificationGraphQLService);
        assertNotNull(locationGraphQLService);
        assertNotNull(eventGraphQLService);
        assertNotNull(grpcNodeService);
        assertNotNull(alertGraphQLService);
        assertNotNull(azureActiveDiscoveryGraphQLService);
        assertNotNull(flowGraphQLService);
	}

}
