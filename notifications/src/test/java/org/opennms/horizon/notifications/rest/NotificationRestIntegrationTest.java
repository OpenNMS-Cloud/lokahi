package org.opennms.horizon.notifications.rest;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.shared.dto.notifications.PagerDutyConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.assertEquals;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(locations = "classpath:application.yml")
@ActiveProfiles("test")
class NotificationRestIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @SpyBean
    private PagerDutyAPI pagerDutyAPI;

    @LocalServerPort
    private Integer port;

    @Test
    void callSaveConfig() throws Exception {
        String integrationKey = "not_verified";

        saveConfig(integrationKey);
    }

    @Test
    void callSaveConfigTwice() throws Exception {
        String integrationKey = "not_verified";

        saveConfig(integrationKey);

        integrationKey = "not_verified2";

        saveConfig(integrationKey);
    }

    private void saveConfig(String integrationKey) {
        PagerDutyConfigDTO config = new PagerDutyConfigDTO(integrationKey);
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<PagerDutyConfigDTO> request = new HttpEntity<>(config, headers);

        ResponseEntity<String> response = this.testRestTemplate
            .withBasicAuth("testUser", "testPassword")
            .postForEntity("http://localhost:" + port + "/notifications/config", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());

        verifyConfigTable(integrationKey);
    }

    private void verifyConfigTable(String integrationKey) {
        String sql = "SELECT integrationKey FROM pager_duty_config";
        List<PagerDutyConfigDTO> configList = null;
        configList = jdbcTemplate.query(
            sql,
            (rs, rowNum) ->
                new PagerDutyConfigDTO(
                    rs.getString("integrationKey")
                )
        );

        PagerDutyConfigDTO config = configList.get(0);

        assertEquals(integrationKey, config.getIntegrationkey());
    }
}
