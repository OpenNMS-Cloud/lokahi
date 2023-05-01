package org.opennms.horizon.minioncertmanager;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import lombok.Getter;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateManagerGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Getter
public class MinionCertificateManagerGrpcClientUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MinionCertificateManagerGrpcClientUtils.class);

    private static final int DEADLINE_DURATION = 30;
    private static final String LOCALHOST = "localhost";


    private Integer externalGrpcPort;
    private String tenantId;

    private final Map<String, String> grpcHeaders = new TreeMap<>();
    private MinionCertificateManagerGrpc.MinionCertificateManagerBlockingStub minionCertificateManagerStub;

    public void externalGRPCPortInSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        externalGrpcPort = Integer.parseInt(value);
        LOG.info("Using External gRPC port {}", externalGrpcPort);
    }

    public void grpcTenantId(String tenantId) {
        Objects.requireNonNull(tenantId);
        this.tenantId = tenantId;
        grpcHeaders.put(GrpcConstants.TENANT_ID_KEY, tenantId);
        LOG.info("Using Tenant Id {}", tenantId);
    }

    public void createGrpcConnection() {
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress(LOCALHOST, externalGrpcPort);

        ManagedChannel managedChannel = channelBuilder.usePlaintext().build();
        managedChannel.getState(true);
        minionCertificateManagerStub = MinionCertificateManagerGrpc.newBlockingStub(managedChannel)
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
