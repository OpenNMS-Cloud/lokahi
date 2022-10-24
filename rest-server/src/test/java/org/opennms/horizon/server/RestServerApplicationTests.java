package org.opennms.horizon.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.service.*;
import org.opennms.horizon.server.service.gateway.PlatformGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RestServerApplicationTests {
	@Autowired
	private PlatformGateway gateway;
	@Autowired
	private AlarmService alarmService;
    @Autowired
    private EventService eventService;
    @Autowired
    private NotificationsService notificationsService;
    @Autowired
    private DataChoicesService dataChoicesService;

	@Test
	void contextLoads() {
		assertNotNull(gateway);
		assertNotNull(alarmService);
        assertNotNull(eventService);
        assertNotNull(notificationsService);
        assertNotNull(dataChoicesService);
	}

}
