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
import org.opennms.horizon.model.common.proto.Severity;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.SpringContextTestInitializer;
import org.opennms.horizon.notifications.api.PagerDutyAPI;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import static org.mockito.ArgumentMatchers.any;
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
class AlertKafkaConsumerIntegrationTestNoConfig {
    private static final int KAFKA_TIMEOUT = 30000;
    private static final int HTTP_TIMEOUT = 5000;

    @Value("${horizon.kafka.alerts.topic}")
    private String alertsTopic;

    private Producer<String, byte[]> kafkaProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @SpyBean
    private AlertKafkaConsumer alertKafkaConsumer;

    @Captor
    ArgumentCaptor<byte[]> alertCaptor;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private RestTemplate restTemplate;

    @SpyBean
    private PagerDutyAPI pagerDutyAPI;

    @LocalServerPort
    private Integer port;

    @BeforeAll
    void setUp() {
        Map<String, Object> producerConfig = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));

        DefaultKafkaProducerFactory<String, byte[]> kafkaProducerFactory
            = new DefaultKafkaProducerFactory<>(producerConfig, new StringSerializer(), new ByteArraySerializer());
        kafkaProducer = kafkaProducerFactory.createProducer();
    }

    @Test
    void testProducingAlertWithNoConfigSetup() throws InvalidProtocolBufferException {
        int id = 1234;
        String tenantId = "opennms-prime";
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

        // This is the call to the PagerDuty API, we won't get this far, as we will get an exception when we try
        // to get the token, as the config table hasn't been setup.
        verify(restTemplate, timeout(HTTP_TIMEOUT).times(0)).exchange(any(URI.class),
            ArgumentMatchers.eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(Class.class));
    }

    @AfterAll
    void shutdown() {
        kafkaProducer.close();
    }
}
