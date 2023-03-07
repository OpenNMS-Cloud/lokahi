package org.opennms.horizon.notifications.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.model.common.proto.Severity;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.SpringContextTestInitializer;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EmbeddedKafka(topics = {
    "${horizon.kafka.alerts.topic}",
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", locations = "classpath:application.yml")
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@ActiveProfiles("test")
class AlertKafkaConsumerIntegrationTest {
    private static final int KAFKA_TIMEOUT = 5000;
    private static final int HTTP_TIMEOUT = 5000;

    @Value("${horizon.kafka.alerts.topic}")
    private String alertsTopic;

    private Producer<String, byte[]> kafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @SpyBean
    private AlertKafkaConsumer alertKafkaConsumer;

    @Autowired
    private NotificationService notificationService;

    @Captor
    ArgumentCaptor<byte[]> alertCaptor;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeAll
    void setUp() {
        Map<String, Object> producerConfig = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));

        DefaultKafkaProducerFactory<String, byte[]> kafkaProducerFactory
            = new DefaultKafkaProducerFactory<>(producerConfig, new StringSerializer(), new ByteArraySerializer());
        kafkaProducer = kafkaProducerFactory.createProducer();
    }

    private void setupConfig() {
        String integrationKey = "not_verified";

        PagerDutyConfigDTO config = PagerDutyConfigDTO.newBuilder().setIntegrationKey(integrationKey).build();
        notificationService.postPagerDutyConfig(config);
    }

    @Test
    void testProducingAlertWithConfigSetup() throws NotificationException, InvalidProtocolBufferException {
        setupConfig();

        int id = 1234;
        Alert alert = Alert.newBuilder()
            .setSeverity(Severity.MINOR)
            .setLogMessage("hello")
            .setDatabaseId(1234)
            .setTenantId("opennms-prime")
            .build();
        var producerRecord = new ProducerRecord<String,byte[]>(alertsTopic, alert.toByteArray());
        kafkaProducer.send(producerRecord);
        kafkaProducer.flush();

        verify(alertKafkaConsumer, timeout(KAFKA_TIMEOUT).times(1))
            .consume(alertCaptor.capture());

        Alert capturedAlert = Alert.parseFrom(alertCaptor.getValue());
        assertEquals(id, capturedAlert.getDatabaseId());

        // This is the call to the PagerDuty API, it will fail due to an invalid token, but we just need to
        // verify that the call has been attempted.
        verify(restTemplate, timeout(HTTP_TIMEOUT).times(1)).exchange(ArgumentMatchers.any(URI.class),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.any(HttpEntity.class),
            ArgumentMatchers.any(Class.class));
    }

    @AfterAll
    void shutdown() {
        kafkaProducer.close();
    }
}
