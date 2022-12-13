package org.opennms.miniongateway.grpc.twin;

import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.miniongateway.grpc.server.ConnectionIdentity;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.api.TaskSetForwarder;
import org.opennms.taskset.service.api.TaskSetListener;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class TaskSetTwinMessageProcessor implements BiConsumer<Identity, StreamObserver<CloudToMinionMessage>> {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetTwinMessageProcessor.class);
    private final TaskSetPublisher publisher;
    private final TaskSetForwarder forwarder;
    private final GrpcTwinPublisher twinPublisher;

    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;

    private final Map<String, Set<ForwardingTaskListener>> listeners = new ConcurrentHashMap<>();
    private final BiConsumer<IpcIdentity, StreamObserver<CloudToMinionMessage>> streamObserver;

    private Logger log = DEFAULT_LOGGER;

    public TaskSetTwinMessageProcessor(TaskSetPublisher publisher, TaskSetForwarder forwarder, GrpcTwinPublisher twinPublisher,
        TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor) {
        this.publisher = publisher;
        this.forwarder = forwarder;
        this.twinPublisher = twinPublisher;
        this.streamObserver = twinPublisher.getStreamObserver();
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
    }

    @Override
    public void accept(Identity minionHeader, StreamObserver<CloudToMinionMessage> cloudToMinionMessageStreamObserver) {
        log.info("Have Message to send to Minion: system-id={}, location={}",
            minionHeader.getSystemId(),
            minionHeader.getLocation());

        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        IpcIdentity identity = new ConnectionIdentity(minionHeader);
        forwarder.addListener(tenantId, minionHeader.getLocation(), new ForwardingTaskListener(tenantId, identity, twinPublisher, forwarder));
        streamObserver.accept(identity, cloudToMinionMessageStreamObserver);
    }

    static class ForwardingTaskListener implements TaskSetListener, AutoCloseable {

        private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ForwardingTaskListener.class);

        private Logger log = DEFAULT_LOGGER;

        private String tenantId;
        private final IpcIdentity identity;
        private final TwinPublisher grpcPublisher;
        private final TaskSetForwarder forwarder;
        private TwinPublisher.Session<TaskSet> session;

        ForwardingTaskListener(String tenantId, IpcIdentity identity, TwinPublisher grpcPublisher, TaskSetForwarder forwarder) {
            this.tenantId = tenantId;
            this.identity = identity;
            this.grpcPublisher = grpcPublisher;
            this.forwarder = forwarder;
        }

        @Override
        public void onTaskSetUpdate(TaskSet taskSet) {
            try {
                session = grpcPublisher.register("task-set", TaskSet.class, identity.getLocation());
                session.publish(taskSet);
            } catch (IOException e) {
                log.error("failed to update task set", e);
            }
        }

        @Override
        public void close() throws Exception {
            if (session != null) {
                session.close();
            }

            forwarder.removeListener(tenantId, identity.getLocation(), this);;
        }
    }
}
