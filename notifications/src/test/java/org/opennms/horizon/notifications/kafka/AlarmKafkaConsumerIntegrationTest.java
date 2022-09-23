package org.opennms.horizon.notifications.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.api.PagerDutyAPIImpl;
import org.opennms.horizon.notifications.api.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
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
    "${horizon.kafka.alarms.topic}",
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = NotificationsApplication.class)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", locations = "classpath:application.yml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlarmKafkaConsumerIntegrationTest {
    private static final int KAFKA_TIMEOUT = 5000;

    @Value("${horizon.kafka.alarms.topic}")
    private String alarmsTopic;

    private Producer<String, String> stringProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @SpyBean
    private AlarmKafkaConsumer alarmKafkaConsumer;

    @Captor
    ArgumentCaptor<AlarmDTO> alarmCaptor;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private RestTemplate restTemplate;

    @SpyBean
    private PagerDutyAPIImpl pagerDutyAPI;

    @LocalServerPort
    private Integer port;

    @BeforeAll
    void setUp() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));

        DefaultKafkaProducerFactory<String, String> stringFactory
            = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new StringSerializer());
        stringProducer = stringFactory.createProducer();
    }

    private void setupConfig() throws NotificationException {
        String integrationKey = "not_verified";

        PagerDutyConfigDTO config = new PagerDutyConfigDTO(integrationKey);
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<PagerDutyConfigDTO> request = new HttpEntity<>(config, headers);

        testRestTemplate.postForEntity("http://localhost:" + port + "/notifications/config", request, String.class);
    }

    @Test
    @Order(2)
    void testProducingAlarmWithConfigSetup() throws NotificationException {
        setupConfig();

        int id = 1234;
        stringProducer.send(new ProducerRecord<>(alarmsTopic, String.format("{\"id\": %d, \"severity\":\"indeterminate\", \"logMessage\":\"hello\"}", id)));
        stringProducer.flush();

        verify(alarmKafkaConsumer, timeout(KAFKA_TIMEOUT).times(1))
            .consume(alarmCaptor.capture());

        AlarmDTO capturedAlarm = alarmCaptor.getValue();
        assertEquals(id, capturedAlarm.getId());

        // This is the call to the PagerDuty API, it will fail due to an invalid token, but we just need to
        // verify that the call has been attempted.
        verify(restTemplate, timeout(KAFKA_TIMEOUT).times(1)).exchange(ArgumentMatchers.any(URI.class),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.any(HttpEntity.class),
            ArgumentMatchers.any(Class.class));
    }

    @Test
    @Order(1)
    void testProducingAlarmWithNoConfigSetup() {
        int id = 1234;
        stringProducer.send(new ProducerRecord<>(alarmsTopic, String.format("{\"id\": %d, \"severity\":\"indeterminate\", \"logMessage\":\"hello\"}", id)));
        stringProducer.flush();

        verify(alarmKafkaConsumer, timeout(KAFKA_TIMEOUT).times(1))
            .consume(alarmCaptor.capture());

        AlarmDTO capturedAlarm = alarmCaptor.getValue();
        assertEquals(id, capturedAlarm.getId());

        // This is the call to the PagerDuty API, we won't get this far, as we will get an exception when we try
        // to get the token, as the config table hasn't been setup.
        verify(restTemplate, timeout(KAFKA_TIMEOUT).times(0)).exchange(ArgumentMatchers.any(URI.class),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.any(HttpEntity.class),
            ArgumentMatchers.any(Class.class));
    }

    @AfterAll
    void shutdown() {
        stringProducer.close();
    }
}
