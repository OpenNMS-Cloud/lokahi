package org.opennms.miniongateway.grpc.twin;

import io.grpc.stub.StreamObserver;
import java.util.List;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskSetTwinMessageProcessor implements OutgoingMessageHandler {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetTwinMessageProcessor.class);

    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;
    private final LocationServerInterceptor locationServerInterceptor;

    private final List<OutgoingMessageFactory> outgoingMessageFactoryList;

    private Logger log = DEFAULT_LOGGER;

    public TaskSetTwinMessageProcessor(TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor, LocationServerInterceptor locationServerInterceptor,
        List<OutgoingMessageFactory> outgoingMessageFactoryList) {
        this.outgoingMessageFactoryList = outgoingMessageFactoryList;
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
        this.locationServerInterceptor = locationServerInterceptor;
    }

    @Override
    public void handleOutgoingStream(Identity minionHeader, StreamObserver<CloudToMinionMessage> cloudToMinionMessageStreamObserver) {
        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        String location = locationServerInterceptor.readCurrentContextLocationId();
        log.info("Have Message to send to Minion: tenant-id: {}; system-id={}, location={}",
            tenantId,
            minionHeader.getSystemId(),
            location
        );

        for (OutgoingMessageFactory outgoingMessageFactory : outgoingMessageFactoryList) {
            // streamObserver.accept(identity, cloudToMinionMessageStreamObserver);
            outgoingMessageFactory.create(minionHeader.getSystemId(), tenantId, location, cloudToMinionMessageStreamObserver);
        }

    }

}
