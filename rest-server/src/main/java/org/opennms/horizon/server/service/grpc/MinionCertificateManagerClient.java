package org.opennms.horizon.server.service.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateManagerGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class MinionCertificateManagerClient {

    private final ManagedChannel minionCertificateManagerChannel;
    private final long deadline;

    private MinionCertificateManagerGrpc.MinionCertificateManagerBlockingStub minionCertStub;

    @PostConstruct
    protected void initialStubs() {
        minionCertStub = MinionCertificateManagerGrpc.newBlockingStub(minionCertificateManagerChannel);
    }

    @PreDestroy
    public void shutdown() {
        if (minionCertificateManagerChannel != null && !minionCertificateManagerChannel.isShutdown()) {
            minionCertificateManagerChannel.shutdown();
        }
    }

    public GetMinionCertificateResponse getMinionCert(String tenantId, Long locationId, String accessToken) {
        MinionCertificateRequest request = MinionCertificateRequest.newBuilder()
            .setTenantId(tenantId)
            .setLocationId(locationId)
            .build();
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return minionCertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getMinionCert(request);
    }

    public void revokeCertificate(String tenantId, Long locationId, String accessToken) {
        MinionCertificateRequest request = MinionCertificateRequest.newBuilder()
            .setTenantId(tenantId)
            .setLocationId(locationId)
            .build();
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        minionCertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).revokeMinionCert(request);
    }
}
