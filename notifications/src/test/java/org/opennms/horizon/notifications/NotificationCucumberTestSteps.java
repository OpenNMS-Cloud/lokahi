package org.opennms.horizon.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.spring.CucumberContextConfiguration;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.stub.MetadataUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.notifications.api.PagerDutyDao;
import org.opennms.horizon.notifications.api.keycloak.KeyCloakAPI;
import org.opennms.horizon.notifications.dto.NotificationServiceGrpc;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.opennms.horizon.notifications.model.MonitoringPolicy;
import org.opennms.horizon.notifications.repository.MonitoringPolicyRepository;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.notifications.tenant.WithTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@CucumberContextConfiguration
@SpringBootTest
@EnableAutoConfiguration
@EmbeddedKafka(partitions = 1)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", locations = "classpath:application.yml")
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

    @Autowired
    @MockBean
    private KeyCloakAPI keyCloakAPI;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    MonitoringPolicyRepository monitoringPolicyRepository;

    @Value("${horizon.kafka.monitoring-policy.topic}")
    private String monitoringPolicyTopic;

    private Producer<String, byte[]> kafkaProducer;

    private Exception caught;

    private Alert alert;

    private String email;
    private HttpClient httpClient = HttpClientBuilder.create().build();

    @Then("tear down grpc setup")
    public void cleanUp() throws InterruptedException {
        afterTest();
    }


    @Given("clean setup")
    public void clean(){
        jdbcTemplate.execute("delete from pager_duty_config");
        jdbcTemplate.execute("delete from monitoring_policy");
        caught = null;
    }

    @Given("grpc setup")
    public void cleanGrpc() throws VerificationException {
        prepareServer();
        serviceStub = NotificationServiceGrpc.newBlockingStub(channel);
    }
    @Given("kafka setup")
    public void setupKafka() {
        Map<String, Object> producerConfig = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));

        DefaultKafkaProducerFactory<String, byte[]> kafkaProducerFactory
            = new DefaultKafkaProducerFactory<>(producerConfig, new StringSerializer(), new ByteArraySerializer());
        kafkaProducer = kafkaProducerFactory.createProducer();
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

    @Given("Alert posted via service with tenant {string} with monitoring policy ID {long}")
    public void postAlertViaService(String tenantId, long monitoringPolicyId) {
        postAlert(tenantId, monitoringPolicyId);
    }

    private void postAlert(String tenantId, long monitoringPolicyId) {
        alert = Alert.newBuilder().setLogMessage("Hello").setDescription("Alarm!").setTenantId(tenantId).addMonitoringPolicyId(monitoringPolicyId).build();
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

    @Given("Alert posted via service with no config with tenant {string} with monitoring policy ID {long}")
    public void postNotificationWithNoConfig(String tenantId, long monitoringPolicyId) {
        caught = null;
        try {
            postAlert(tenantId, monitoringPolicyId);
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

    @Given("the following monitoring policies sent via Kafka")
    public void addMonitoringPolicies(DataTable table) {
        table.asLists().forEach((row) -> {
            MonitorPolicyProto proto = MonitorPolicyProto.newBuilder()
                .setId(Long.parseLong(row.get(0)))
                .setTenantId(row.get(1))
                .setNotifyByPagerDuty(Boolean.parseBoolean(row.get(2)))
                .setNotifyByEmail(Boolean.parseBoolean(row.get(3)))
                .setNotifyByWebhooks(Boolean.parseBoolean(row.get(4)))
                .build();
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(monitoringPolicyTopic, proto.toByteArray());
            kafkaProducer.send(record);
        });
        kafkaProducer.flush();
    }


    @Then("verify {string} has a monitoring policy with ID {long} and the following enabled")
    @WithTenant(tenantIdArg = 0)
    public void verifyMonitoringPolicy(String tenant, long id, List<String> enabledNotifications) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MonitoringPolicy> monitoringPolicy = monitoringPolicyRepository.findByTenantIdAndIdIn(tenant, List.of(id));
            assertEquals(1, monitoringPolicy.size());

            assertEquals(enabledNotifications.contains("PagerDuty"), monitoringPolicy.get(0).isNotifyByPagerDuty());
            assertEquals(enabledNotifications.contains("email"), monitoringPolicy.get(0).isNotifyByEmail());
            assertEquals(enabledNotifications.contains("webhooks"), monitoringPolicy.get(0).isNotifyByWebhooks());
        });
    }

    @Given("{string} has a monitoring policy with ID {long}")
    @WithTenant(tenantIdArg = 0)
    public void waitForMonitoringPolicy(String tenant, long id) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !monitoringPolicyRepository.findByTenantIdAndIdIn(tenant, List.of(id)).isEmpty());
    }

    @Given("{string} has email {string}")
    public void setTenantEmail(String tenant, String email) {
        this.email = email;
        when(keyCloakAPI.getTenantEmailAddresses(tenant)).thenReturn(List.of(email));
    }

    @Then("verify alert is sent by email")
    public void verifyEmailAlert() throws Exception {
        // Mailhog exposes an API with all emails recieved
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            HttpGet get = new HttpGet(String.format(
                "http://%s:%d/api/v2/messages",
                SpringContextTestInitializer.mailhog.getHost(),
                SpringContextTestInitializer.mailhog.getMappedPort(SpringContextTestInitializer.MAILHOG_WEB_PORT)
            ));

            JsonNode nodes = new ObjectMapper().readTree(httpClient.execute(get).getEntity().getContent());
            assertEquals(1, nodes.get("total").asInt());
            JsonNode content = nodes.get("items").get(0).get("Content");
            // Check the email is addressed to the tenant, and the body and subject aren't empty.
            assertEquals(List.of(email), StreamSupport.stream(content.get("Headers").get("To").spliterator(), false).map(JsonNode::asText).toList());
            assertNotEquals("", content.get("Headers").get("Subject").get(0).asText());
            assertNotEquals("", content.get("Body").asText());
        });
    }
}
