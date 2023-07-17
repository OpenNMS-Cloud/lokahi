package org.opennms.horizon.alertservice;

import java.util.concurrent.TimeUnit;

import org.opennms.horizon.alert.tag.proto.TagServiceGrpc;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionServiceGrpc;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

public class AlertGrpcClientUtils {
    private static final int DEADLINE_DURATION = 30000;
    private static final String LOCALHOST = "localhost";

    private final DynamicTenantIdInterceptor dynamicTenantIdInterceptor = new DynamicTenantIdInterceptor(
        // Pull private key directly from container
        CucumberRunnerIT.testContainerRunnerClassRule.getJwtKeyPair());
    private AlertServiceGrpc.AlertServiceBlockingStub alertServiceStub;
    private MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub policyStub;
    private TagServiceGrpc.TagServiceBlockingStub tagStub;
    private AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceBlockingStub alertEventDefinitionStub;

    public AlertGrpcClientUtils() {
        initStubs();
    }

    private void initStubs () {
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress(LOCALHOST,
                // Pull gRPC server port directly from container
                CucumberRunnerIT.testContainerRunnerClassRule.getGrpcPort());

        ManagedChannel managedChannel = channelBuilder.usePlaintext().build();
        managedChannel.getState(true);
        alertServiceStub = AlertServiceGrpc.newBlockingStub(managedChannel)
            .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
            .withInterceptors(dynamicTenantIdInterceptor);
        policyStub = MonitorPolicyServiceGrpc.newBlockingStub(managedChannel)
            .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
            .withInterceptors(dynamicTenantIdInterceptor);
        tagStub = TagServiceGrpc.newBlockingStub(managedChannel)
            .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
            .withInterceptors(dynamicTenantIdInterceptor);
        alertEventDefinitionStub = AlertEventDefinitionServiceGrpc.newBlockingStub(managedChannel)
            .withDeadlineAfter(DEADLINE_DURATION, TimeUnit.SECONDS)
            .withInterceptors(dynamicTenantIdInterceptor);
    }

    public void setTenantId(String tenantId) {
        dynamicTenantIdInterceptor.setTenantId(tenantId);
    }

    public AlertServiceGrpc.AlertServiceBlockingStub getAlertServiceStub() {
        return alertServiceStub;
    }

    public MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub getPolicyStub() {
        return policyStub;
    }

    public TagServiceGrpc.TagServiceBlockingStub getTagStub() {
        return tagStub;
    }

    public AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceBlockingStub getAlertEventDefinitionStub() {
        return alertEventDefinitionStub;
    }
}
