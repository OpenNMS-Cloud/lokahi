package org.opennms.miniongateway.grpc.twin;

import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.miniongateway.grpc.server.ConnectionIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class TaskSetTwinMessageProcessor implements BiConsumer<Identity, StreamObserver<CloudToMinionMessage>> {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetTwinMessageProcessor.class);

    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;

    private final BiConsumer<IpcIdentity, StreamObserver<CloudToMinionMessage>> streamObserver;

    private Logger log = DEFAULT_LOGGER;

    public TaskSetTwinMessageProcessor(GrpcTwinPublisher twinPublisher, TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor) {
        this.streamObserver = twinPublisher.getStreamObserver(tenantIDGrpcServerInterceptor);
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
    }

    @Override
    public void accept(Identity minionHeader, StreamObserver<CloudToMinionMessage> cloudToMinionMessageStreamObserver) {
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        log.info("Have Message to send to Minion: tenant-id: {}; system-id={}, location={}",
            tenantId,
            minionHeader.getSystemId(),
            minionHeader.getLocation());

        IpcIdentity identity = new ConnectionIdentity(minionHeader);
        streamObserver.accept(identity, cloudToMinionMessageStreamObserver);
    }

}
