package org.opennms.horizon.notifications;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(locations = "classpath:application.yml")
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class NotificationsApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @LocalServerPort
    private Integer port;

	@Test
	void contextLoads() {
        System.out.println(environment.getProperty("spring.application.name"));
	}
}
