package org.opennms.horizon.notifications;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.spring.CucumberContextConfiguration;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.stub.MetadataUtils;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.api.PagerDutyDao;
import org.opennms.horizon.notifications.dto.NotificationServiceGrpc;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationAPIRetryableException;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.notifications.tenant.WithTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@CucumberContextConfiguration
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class NotificationCucumberTestSteps extends GrpcTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationCucumberTestSteps.class);

    private NotificationServiceGrpc.NotificationServiceBlockingStub serviceStub;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PagerDutyDao pagerDutyDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @MockBean
    @SpyBean
    public RestTemplate restTemplate;

    private Exception caught;

    @Then("tear down grpc setup")
    public void cleanUp() throws InterruptedException {
        afterTest();
    }


    @Given("clean setup")
    public void clean(){
        jdbcTemplate.execute("delete from pager_duty_config");
        caught = null;
    }

    @Given("grpc setup")
    public void cleanGrpc() throws VerificationException {
        prepareServer();
        serviceStub = NotificationServiceGrpc.newBlockingStub(channel);
    }

    @Given("Integration {string} key set to {string} via grpc")
    public void setIntegrationKeyGrpc(String tenantId, String key) throws VerificationException {
        saveConfig(tenantId, key);

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Given("Integration {string} key set to {string} via grpc with token tenant {string}")
    public void setIntegrationKeyGrpcDifferentTenant(String tenantId, String key, String differentTenantId) throws VerificationException {
        caught = null;
        try {
            saveConfigWithDifferentTokenTenant(tenantId, key, differentTenantId);
        } catch (Exception ex) {
            caught = ex;
        }
    }

    @Given("Integration {string} key set to {string} then {string} via grpc")
    public void setIntegrationKeyGrpcTwice(String tenantId, String key, String secondKey) throws VerificationException {
        saveConfig(tenantId, key);
        saveConfig(tenantId, secondKey);

        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Given("Integration {string} key set to {string}, then Integration {string} key set to {string} via grpc")
    public void setIntegrationKeyGrpcTwiceWithDifferentTenants(String tenantId, String key, String otherTenantId, String secondKey) throws VerificationException {
        saveConfig(tenantId, key);
        saveConfig(otherTenantId, secondKey);

        verify(spyInterceptor, times(1)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(1)).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    private void saveConfig(String tenantId, String key) {
        String header = getAuthHeader(tenantId);
        PagerDutyConfigDTO config = PagerDutyConfigDTO.newBuilder().setIntegrationKey(key).setTenantId(tenantId).build();

        serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(header)))
            .postPagerDutyConfig(config);
    }

    private void saveConfigWithDifferentTokenTenant(String tenantId, String key, String tokenTenantId) {
        String header = getAuthHeader(tokenTenantId);
        PagerDutyConfigDTO config = PagerDutyConfigDTO.newBuilder().setIntegrationKey(key).setTenantId(tenantId).build();

        serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(header)))
            .postPagerDutyConfig(config);
    }

    private String getAuthHeader(String tenantId) {
        if ("test-tenant".equals(tenantId)) {
            return authHeader;
        } else if ("other-tenant".equals(tenantId)) {
            return differentTenantHeader;
        } else {
            throw new RuntimeException("Invalid tenant: " + tenantId);
        }
    }

    @Given("Integration key set to {string} without tenantId")
    public void setIntegrationKey(String key){
        caught = null;
        try {
            PagerDutyConfigDTO configDTO = PagerDutyConfigDTO.newBuilder().setIntegrationKey(key).build();
            notificationService.postPagerDutyConfig(configDTO);
        } catch (Exception e) {
            caught = e;
        }
    }

    @Given("Alert posted via service with tenant {string}")
    public void postAlertViaService(String tenantId) throws Exception{
        postAlert(tenantId);
    }

    private void postAlert(String tenantId) throws NotificationException {
        Alert alert = Alert.newBuilder().setLogMessage("Hello").setTenantId(tenantId).build();
        notificationService.postNotification(alert);
    }

    @Then("verify {string} key is {string}")
    @WithTenant(tenantIdArg = 0)
    public void verifyKey(String tenantId, String key) {
        try {
            PagerDutyConfigDTO configDTO = pagerDutyDao.getConfig(tenantId);
            assertEquals(key, configDTO.getIntegrationKey());
        } catch (NotificationConfigUninitializedException e) {
            fail("Config is not initialised as expected");
        }
    }

    @Then("verify {string} key is not set")
    @WithTenant(tenantIdArg = 0)
    public void verifyKeyIsNotSet(String tenantId) {
        try {
            pagerDutyDao.getConfig(tenantId);
            fail("Config should not be initialised");
        } catch (NotificationConfigUninitializedException e) {
            assertEquals("PagerDuty config not initialized. Row count=0", e.getMessage());
        }
    }

    @Then("verify pager duty rest method is called {int} times")
    public void verifyPagerDutyAPICalled(int count) {
        verify(restTemplate, times(count)).exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Given("Alert posted via service with no config with tenant {string}")
    public void postNotificationWithNoConfig(String tenantId) {
        caught = null;
        try {
            postAlert(tenantId);
        } catch (Exception ex) {
            caught = ex;
        }
    }

    @Then("verify exception {string} thrown with message {string}")
    public void verifyException(String exceptionName, String message) {
        if (caught == null) {
            fail("No exception caught");
        } else {
            assertEquals(exceptionName, caught.getClass().getSimpleName(), "Exception mismatch");
            assertEquals(message, caught.getMessage());
        }
    }

    @Given("first attempt to post to PagerDuty will fail but should retry")
    public void mockPagerDutyFailOnce() {
        when(restTemplate.exchange(any(), any(), any(), any(Class.class)))
            .thenThrow(new RestClientResponseException("Failed", HttpStatus.BAD_GATEWAY, "Failed", null, null, null))
            .thenReturn(ResponseEntity.ok(null));
    }
}
