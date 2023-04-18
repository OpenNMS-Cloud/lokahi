package org.opennms.horizon.notifications.grpc.service;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.notifications.dto.NotificationServiceGrpc;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.notifications.tenant.TenantLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {
    private final TenantLookup tenantLookup;
    private final NotificationService notificationService;

    @Override
    public void postPagerDutyConfig(PagerDutyConfigDTO request, StreamObserver<PagerDutyConfigDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId();

        tenantIdOptional.ifPresentOrElse(tenantId -> {
            notificationService.postPagerDutyConfig(request);
            responseObserver.onNext(request);
            responseObserver.onCompleted();
        }, () -> {
            Status status = Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Tenant Id can't be empty")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        });
    }
}
