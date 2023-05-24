package org.opennms.horizon.metrics;


import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.document.FlowDocumentLog;
import org.opennms.horizon.flows.FlowProcessorTestConfig;
import org.opennms.horizon.flows.grpc.client.GrpcIngesterMockServer;
import org.opennms.horizon.flows.grpc.client.GrpcInventoryMockServer;
import org.opennms.horizon.flows.grpc.client.IngestorApplicationConfig;
import org.opennms.horizon.flows.grpc.client.InventoryApplicationConfig;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

@Disabled // TODO: REPLACE WITH CUCUMBER TEST
@SpringBootTest
@ContextConfiguration(classes = {IngestorApplicationConfig.class, InventoryApplicationConfig.class,
    FlowProcessorTestConfig.class})
@EmbeddedKafka(brokerProperties = {"listeners=PLAINTEXT://localhost:59092", "port=59092"}, topics = "flows-test")
@DirtiesContext
@TestPropertySource(locations = "/application-test.yml")
@ActiveProfiles("test")
class FlowProcessorIntegrationTest {

    @ClassRule
    public static final GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

    public static GrpcIngesterMockServer grpcIngesterMockServer = new GrpcIngesterMockServer();

    public static GrpcInventoryMockServer grpcInventoryMockServer = new GrpcInventoryMockServer();

    @Value("${kafka.flow-topics}")
    private String flowTopic;

    @Autowired
    @Qualifier("kafkaByteArrayProducerTemplate")
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(FlowProcessorIntegrationTest.class);

    private static final String LOCATION1 = "test-location1";
    private static final String LOCATION2 = "test-location2";

    private static Server ingestorServer;

    private static Server inventoryServer;

    @BeforeAll
    public static void setUpMockServers() throws IOException {
        ingestorServer = InProcessServerBuilder.forName(IngestorApplicationConfig.SERVER_NAME)
            .directExecutor()
            .addService(grpcIngesterMockServer)
            .build().start();
        grpcCleanupRule.register(ingestorServer);

        inventoryServer = InProcessServerBuilder.forName(InventoryApplicationConfig.SERVER_NAME)
            .directExecutor()
            .addService(grpcInventoryMockServer)
            .build().start();
        grpcCleanupRule.register(inventoryServer);
    }

    @AfterAll
    public static void tearDown() {
        if (!ingestorServer.isShutdown()) {
            ingestorServer.shutdownNow();
        }
        if (!inventoryServer.isShutdown()) {
            inventoryServer.shutdownNow();
        }
    }

    @Test
    @DirtiesContext
    public void verifyKafkaReceivesMessagesFromProducerAndSendThemToInventoryAndIngester() {
        // Given
        String tenantId = "tenant-id";
        TaskResult testTaskResult = TaskResult.newBuilder().build();
        TaskSetResults testTaskSetResults = TaskSetResults.newBuilder().addResults(testTaskResult).build();

        FlowDocumentLog flows = FlowDocumentLog.newBuilder().addMessage(FlowDocument.newBuilder()
            .setSrcAddress("127.0.0.1")
            .setDstAddress("8.8.8.8")
            .setLocation(LOCATION1)
            .setExporterAddress("127.0.0.1")
        ).addMessage(FlowDocument.newBuilder()
            .setSrcAddress("192.168.0.1")
            .setDstAddress("1.1.1.1")
            .setLocation(LOCATION2)
            .setExporterAddress("127.0.0.1")
        ).build();

        ProducerRecord<String, byte[]> producerRecord = formatProducerRecord(flows.toByteArray(), tenantId);
        LOG.info("sending payload='{}' to topic='{}'", producerRecord, flowTopic);


        // When
        CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(producerRecord);
        future.whenComplete((result, ex) -> Optional.ofNullable(ex)
            .ifPresentOrElse(val -> LOG.error("Unable to send message=[{}] due to: {}", testTaskSetResults.toByteArray(), val.getMessage()),
                () -> LOG.info("Sent message=[{}] with offset=[{}]", testTaskSetResults.toByteArray(), result.getRecordMetadata().offset())));

        // Then
        // Flow Documents should be persisted
        await()
            .await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> grpcIngesterMockServer.isFlowDocumentPersisted());
        Assertions.assertEquals(String.format("%s,%s", tenantId, tenantId), grpcIngesterMockServer.getSavedTenantId());

        // Flow Documents are sent to Inventory for Enrichment
        Map<String, NodeIdQuery> incomingNodeIdQueries = grpcInventoryMockServer.getIncomingNodeIdQueries();
        Assertions.assertEquals(2, incomingNodeIdQueries.size());
        Assertions.assertNotNull(incomingNodeIdQueries.get(LOCATION1));
        Assertions.assertNotNull(incomingNodeIdQueries.get(LOCATION2));
    }

    private ProducerRecord<String, byte[]> formatProducerRecord(byte[] rawContent, String tenantId) {
        List<Header> headers = new LinkedList<>();
        headers.add(new RecordHeader(tenantId, tenantId.getBytes(StandardCharsets.UTF_8)));

        return new ProducerRecord<String, byte[]>(
            flowTopic,
            null,
            null,
            rawContent,
            headers
        );
    }
}
