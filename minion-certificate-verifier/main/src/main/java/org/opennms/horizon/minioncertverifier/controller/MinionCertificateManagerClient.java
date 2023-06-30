package org.opennms.horizon.minioncertverifier.controller;

import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidRequest;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateManagerGrpc;

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

    public IsCertificateValidResponse isCertValid(String serialNumber) {
        IsCertificateValidRequest request = IsCertificateValidRequest.newBuilder().setSerialNumber(serialNumber).build();
        return minionCertStub.withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).isCertValid(request);
    }
}
