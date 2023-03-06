package org.opennms.horizon.notifications;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.spring.CucumberContextConfiguration;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.stub.MetadataUtils;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.notifications.api.PagerDutyDao;
import org.opennms.horizon.notifications.dto.NotificationServiceGrpc;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Given("Integration {string} key set to {string} then {string} via grpc")
    public void setIntegrationKeyGrpcTwice(String tenantId, String key, String secondKey) throws VerificationException {
        saveConfig(tenantId, key);
        saveConfig(tenantId, secondKey);

        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Given("Integration {string} then {string} key set to {string} then {string} via grpc")
    public void setIntegrationKeyGrpcTwiceWithDifferentTenants(String tenantId, String otherTenantId, String key, String secondKey) throws VerificationException {
        saveConfig(tenantId, key);
        saveConfig(otherTenantId, secondKey);

        verify(spyInterceptor, times(1)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(1)).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    private void saveConfig(String tenantId, String key) {
        String header = getAuthHeader(tenantId);
        PagerDutyConfigDTO config = PagerDutyConfigDTO.newBuilder().setIntegrationKey(key).build();

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

    @Given("Integration key set to {string}")
    public void setIntegrationKey(String key){
        PagerDutyConfigDTO configDTO = PagerDutyConfigDTO.newBuilder().setIntegrationKey(key).build();
        notificationService.postPagerDutyConfig(configDTO);
    }

    @Given("Alarm posted via service")
    public void postAlarmViaService() throws Exception{
        postAlarm();
    }

    private void postAlarm() throws NotificationException {
        AlarmDTO alarm = new AlarmDTO();
        alarm.setLogMessage("Hello");
        notificationService.postNotification(alarm);
    }

    @Then("verify key is {string}")
    public void verifyKey(String key) throws Exception {
        PagerDutyConfigDTO configDTO = pagerDutyDao.getConfig();
        assertEquals(key, configDTO.getIntegrationKey());
    }

    @Then("verify {string} key is {string}")
    public void verifyKey(String tenantId, String key) {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            try {
                PagerDutyConfigDTO configDTO = pagerDutyDao.getConfig();
                assertEquals(key, configDTO.getIntegrationKey());
            } catch (NotificationConfigUninitializedException e) {
                fail("Config is not initialised as expected");
            }
        });
    }

    @Then("verify {string} key is not set")
    public void verifyKeyIsNotSet(String tenantId) {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            try {
                pagerDutyDao.getConfig();
                fail("Config should not be initialised");
            } catch (NotificationConfigUninitializedException e) {
                assertEquals("PagerDuty config not initialized. Row count=0", e.getMessage());
            }
        });
    }

    @Then("verify pager duty rest method is called")
    public void verifyPagerDutyAPICalled() {
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Given("Alarm posted via service with no config")
    public void postNotificationWithNoConfig() {
        try {
            postAlarm();
        } catch (Exception ex) {
            caught = ex;
        }
    }

    @Then("verify exception {string} thrown")
    public void verifyException(String exceptionName) {
        if (caught == null) {
            fail("No exception caught");
        } else {
            assertEquals(exceptionName, caught.getClass().getSimpleName(), "Exception mismatch");
        }
    }
}
