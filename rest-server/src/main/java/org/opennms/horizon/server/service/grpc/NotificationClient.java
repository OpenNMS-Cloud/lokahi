package org.opennms.horizon.server.service.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.notifications.dto.NotificationServiceGrpc;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.shared.constants.GrpcConstants;

@RequiredArgsConstructor
public class NotificationClient {
    private final ManagedChannel channel;

    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationStub;

    protected void initialStubs() {
        notificationStub = NotificationServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public PagerDutyConfigDTO postPagerDutyConfig(PagerDutyConfigDTO configDTO, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return notificationStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).postPagerDutyConfig(configDTO);
    }
}
