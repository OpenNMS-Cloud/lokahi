package org.opennms.horizon.notifications.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.opennms.horizon.notifications.NotificationsApplication;
import org.opennms.horizon.notifications.SpringContextTestInitializer;
import org.opennms.horizon.notifications.api.PagerDutyAPIImpl;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
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
import java.nio.charset.StandardCharsets;
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
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@ActiveProfiles("test")
class AlarmKafkaConsumerIntegrationTestNoConfig {
    private static final int KAFKA_TIMEOUT = 30000;
    private static final int HTTP_TIMEOUT = 5000;

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
        Map<String, Object> producerConfig = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));

        DefaultKafkaProducerFactory<String, String> stringFactory
            = new DefaultKafkaProducerFactory<>(producerConfig, new StringSerializer(), new StringSerializer());
        stringProducer = stringFactory.createProducer();
    }

    @Test
    void testProducingAlarmWithNoConfigSetup() {
        int id = 1234;
        String tenantId = "opennms-prime";
        ProducerRecord producerRecord = new ProducerRecord<>(alarmsTopic, String.format("{\"id\": %d, \"severity\":\"indeterminate\", \"logMessage\":\"hello\"}", id));
        producerRecord.headers().add(new RecordHeader(GrpcConstants.TENANT_ID_KEY, tenantId.getBytes(StandardCharsets.UTF_8)));

        stringProducer.send(producerRecord);
        stringProducer.flush();

        verify(alarmKafkaConsumer, timeout(KAFKA_TIMEOUT).times(1))
            .consume(alarmCaptor.capture(),any());

        AlarmDTO capturedAlarm = alarmCaptor.getValue();
        assertEquals(id, capturedAlarm.getId());

        // This is the call to the PagerDuty API, we won't get this far, as we will get an exception when we try
        // to get the token, as the config table hasn't been setup.
        verify(restTemplate, timeout(HTTP_TIMEOUT).times(0)).exchange(any(URI.class),
            ArgumentMatchers.eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(Class.class));
    }

    @AfterAll
    void shutdown() {
        stringProducer.close();
    }
}
