package org.opennms.horizon.inventory.cucumber;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import lombok.Getter;
import org.opennms.horizon.inventory.dto.ConfigurationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Getter
public class InventoryBackgroundHelper {
    protected static final Logger LOG = LoggerFactory.getLogger(InventoryBackgroundHelper.class);
    private static final int DEADLINE_DURATION = 30;
    private Integer externalGrpcPort;
    private String kafkaBootstrapUrl;
    private String tenantId;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub monitoringSystemStub;
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub monitoringLocationStub;
    private NodeServiceGrpc.NodeServiceBlockingStub nodeServiceBlockingStub;
    private ConfigurationServiceGrpc.ConfigurationServiceBlockingStub configurationServiceBlockingStub;
    private TagServiceGrpc.TagServiceBlockingStub tagServiceBlockingStub;

    private final Map<String, String> grpcHeaders = new TreeMap<>();

    public void externalGRPCPortInSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        externalGrpcPort = Integer.parseInt(value);
        LOG.info("Using External gRPC port {}", externalGrpcPort);
    }

    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        kafkaBootstrapUrl = System.getProperty(systemPropertyName);
        LOG.info("Using Kafka Bootstrap URL {}", kafkaBootstrapUrl);
    }

    public void grpcTenantId(String tenantId) {
        Objects.requireNonNull(tenantId);
        this.tenantId = tenantId;
        grpcHeaders.put(GrpcConstants.TENANT_ID_KEY, tenantId);
        LOG.info("Using Tenant Id {}", tenantId);
    }

    public void createGrpcConnectionForInventory() {
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        ManagedChannel managedChannel = channelBuilder.usePlaintext().build();
        managedChannel.getState(true);
        monitoringSystemStub = MonitoringSystemServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS);
        monitoringLocationStub = MonitoringLocationServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS);
        nodeServiceBlockingStub = NodeServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS);
        nodeServiceBlockingStub = NodeServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS);
        configurationServiceBlockingStub = ConfigurationServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS);
        tagServiceBlockingStub = TagServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS);
    }

    private ClientInterceptor prepareGrpcHeaderInterceptor() {
        return MetadataUtils.newAttachHeadersInterceptor(prepareGrpcHeaders());
    }

    private Metadata prepareGrpcHeaders() {
        Metadata result = new Metadata();
        result.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(true));
        result.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        return result;
    }
}
