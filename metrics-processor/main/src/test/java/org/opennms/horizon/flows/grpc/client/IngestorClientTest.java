package org.opennms.horizon.flows.grpc.client;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opennms.dataplatform.flows.document.Direction;
import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.document.Locality;
import org.opennms.dataplatform.flows.document.NetflowVersion;
import org.opennms.dataplatform.flows.document.NodeInfo;
import org.opennms.dataplatform.flows.document.SamplingAlgorithm;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = IngestorApplicationConfig.class)
@ContextConfiguration(classes = MetricRegistry.class)
@TestPropertySource(locations = "classpath:application.yml")
@DirtiesContext
class IngestorClientTest {

    @Autowired
    private IngestorClient ingestorClient;

    @Rule
    public final GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

    @Autowired
    public GrpcIngesterMockServer grpcIngesterMockServer;

    @Autowired
    public ManagedChannel ingestorChannel;

    private Server server;

    @BeforeEach
    public void setUp() throws IOException {
        server = InProcessServerBuilder.forName(IngestorApplicationConfig.SERVER_NAME)
            .directExecutor()
            .addService(grpcIngesterMockServer)
            .build().start();
        grpcCleanupRule.register(server);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // assume channel and server are not null
        server.shutdownNow();
        ingestorChannel.shutdown();
        // fail the test if cleanup is not successful
        assert server.awaitTermination(5, TimeUnit.SECONDS) : "server failed to shutdown";
        assert ingestorChannel.awaitTermination(5, TimeUnit.SECONDS) : "ingestorChannel failed to shutdown";
    }

    @Test
    @DirtiesContext
    void sendData() {
        // Given
        FlowDocument flowDocument = createFlowDocument("test-ip", "test-dest-ip");
        StoreFlowDocumentsRequest storeFlowDocumentsRequest = StoreFlowDocumentsRequest.newBuilder()
            .addDocuments(flowDocument)
            .build();

        // When
        assertDoesNotThrow(() ->
            ingestorClient.sendData(storeFlowDocumentsRequest, "test-tenant-id"));

        // Then
        assertTrue(grpcIngesterMockServer.isFlowDocumentPersisted());
        assertEquals(getFlowDocumentsTenantIds(storeFlowDocumentsRequest), grpcIngesterMockServer.getSavedTenantId());
    }


    public static FlowDocument createFlowDocument(String sourceIp, String destIp) {
        final var now = Instant.now();

        final var flow = FlowDocument.newBuilder()
            .setTimestamp(now.toEpochMilli())
            .setFirstSwitched(UInt64Value.of(now.minus(20_000L, ChronoUnit.MILLIS).toEpochMilli()))
            .setDeltaSwitched(UInt64Value.of(now.minus(10_000L, ChronoUnit.MILLIS).toEpochMilli()))
            .setLastSwitched(UInt64Value.of(now.minus(5_000L, ChronoUnit.MILLIS).toEpochMilli()))
            .setSrcAddress(sourceIp)
            .setSrcPort(UInt32Value.of(510))
            .setDstAddress(destIp)
            .setDstPort(UInt32Value.of(80))
            .setProtocol(UInt32Value.of(6))
            .setDestNode(NodeInfo.newBuilder().addAllCategories(Arrays.asList("any", "more", "last")).setForeignSource("foreign source"))
            .setSrcNode(NodeInfo.newBuilder().getDefaultInstanceForType())
            .setExporterNode(NodeInfo.newBuilder().getDefaultInstanceForType())
            .setApplication("any application")
            .setDirection(Direction.INGRESS)
            .setHost("any host")
            .setSamplingAlgorithm(SamplingAlgorithm.UNASSIGNED)
            .setLocation("test-location")
            .setDstLocality(Locality.PUBLIC)
            .setNetflowVersion(NetflowVersion.V5)
            .setSrcLocality(Locality.PUBLIC)
            .setFlowLocality(Locality.PUBLIC)
            .setTenantId("test-tenant-id");

        return flow.build();
    }

    public static String getFlowDocumentsTenantIds(StoreFlowDocumentsRequest request) {
        return request.getDocumentsList().stream()
            .map(FlowDocument::getTenantId)
            .collect(Collectors.joining(","));
    }
}
