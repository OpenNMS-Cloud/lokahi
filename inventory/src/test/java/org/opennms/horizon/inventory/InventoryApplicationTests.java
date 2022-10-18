package org.opennms.horizon.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = InventoryApplication.class)
@TestPropertySource(locations = "classpath:application.yml")
class InventoryApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        assertNotNull(environment.getProperty("spring.application.name"));
    }
}
