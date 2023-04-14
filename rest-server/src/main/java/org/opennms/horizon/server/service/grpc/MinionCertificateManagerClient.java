package org.opennms.horizon.server.service.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateRequest;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateManagerGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@RequiredArgsConstructor
public class MinionCertificateManagerClient {

    private final ManagedChannel minionCertificateManagerChannel;

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

    public GetMinionCertificateResponse getMinionCert(String tenantId, String location, String accessToken) {
        GetMinionCertificateRequest request = GetMinionCertificateRequest.newBuilder()
            .setTenantId(tenantId)
            .setLocation(location)
            .build();
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return minionCertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getMinionCert(request);
    }
}