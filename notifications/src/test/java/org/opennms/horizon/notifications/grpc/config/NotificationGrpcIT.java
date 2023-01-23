package org.opennms.horizon.notifications.grpc.config;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.dto.NotificationServiceGrpc;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(locations = "classpath:application.yml")
@ActiveProfiles("test")
class NotificationGrpcIT extends GrpcTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private NotificationServiceGrpc.NotificationServiceBlockingStub serviceStub;

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = NotificationServiceGrpc.newBlockingStub(channel);
    }
    @AfterEach
    public void cleanUp() throws InterruptedException {
        afterTest();
    }

    @Test
    void callSaveConfig() throws Exception {
        String integrationKey = "not_verified";

        saveConfig(integrationKey);

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void callSaveConfigTwice() throws Exception {
        String integrationKey = "not_verified";

        saveConfig(integrationKey);

        integrationKey = "not_verified2";

        saveConfig(integrationKey);

        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    private void saveConfig(String integrationKey) throws VerificationException {
        PagerDutyConfigDTO config = PagerDutyConfigDTO.newBuilder().setIntegrationKey(integrationKey).build();

        serviceStub.withInterceptors(MetadataUtils
            .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .postPagerDutyConfig(config);

        verifyConfigTable(integrationKey);
    }

    private void verifyConfigTable(String integrationKey) {
        String sql = "SELECT integrationKey FROM pager_duty_config";
        List<PagerDutyConfigDTO> configList = null;
        configList = jdbcTemplate.query(
            sql,
            (rs, rowNum) ->
                PagerDutyConfigDTO.newBuilder().setIntegrationKey(
                    rs.getString("integrationKey")
                ).build()
        );

        PagerDutyConfigDTO config = configList.get(0);

        assertEquals(integrationKey, config.getIntegrationKey());
    }
}
