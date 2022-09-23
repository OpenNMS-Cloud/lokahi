package org.opennms.horizon.notifications.rest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.notifications.api.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationBadDataException;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(locations = "classpath:application.yml")
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
    void callSaveInvalidConfig() throws Exception {
        Mockito.doThrow(new NotificationBadDataException("Invalid PagerDuty token")).when(pagerDutyAPI).validateConfig(any());

        PagerDutyConfigDTO config = new PagerDutyConfigDTO("invalid_token", "not_verified");
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<PagerDutyConfigDTO> request = new HttpEntity<>(config, headers);

        ResponseEntity<String> response = this.testRestTemplate.postForEntity("http://localhost:" + port + "/notifications/config", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void callSaveConfig() throws Exception {
        Mockito.doNothing().when(pagerDutyAPI).validateConfig(any());

        String token = "unverified_token";
        String integrationKey = "not_verified";

        saveConfig(token, integrationKey);
    }

    @Test
    void callSaveConfigTwice() throws Exception {
        Mockito.doNothing().when(pagerDutyAPI).validateConfig(any());

        String token = "unverified_token";
        String integrationKey = "not_verified";

        saveConfig(token, integrationKey);

        token = "unverified_token2";
        integrationKey = "not_verified2";

        saveConfig(token, integrationKey);
    }

    private void saveConfig(String token, String integrationKey) {
        PagerDutyConfigDTO config = new PagerDutyConfigDTO(token, integrationKey);
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<PagerDutyConfigDTO> request = new HttpEntity<>(config, headers);

        ResponseEntity<String> response = this.testRestTemplate.postForEntity("http://localhost:" + port + "/notifications/config", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());

        verifyConfigTable(token, integrationKey);
    }

    private void verifyConfigTable(String token, String integrationKey) {
        String sql = "SELECT token, integrationKey FROM pager_duty_config";
        List<PagerDutyConfigDTO> configList = null;
        configList = jdbcTemplate.query(
            sql,
            (rs, rowNum) ->
                new PagerDutyConfigDTO(
                    rs.getString("token"),
                    rs.getString("integrationKey")
                )
        );

        PagerDutyConfigDTO config = configList.get(0);

        assertEquals(token, config.getToken());
        assertEquals(integrationKey, config.getIntegrationkey());
    }
}
