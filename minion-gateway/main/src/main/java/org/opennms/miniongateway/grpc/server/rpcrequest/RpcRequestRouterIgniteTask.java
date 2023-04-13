package org.opennms.miniongateway.grpc.server.rpcrequest;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeJobResultPolicy;
import org.apache.ignite.compute.ComputeTask;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.resources.SpringResource;
import org.jetbrains.annotations.NotNull;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.miniongateway.detector.server.IgniteRpcRequestDispatcher;
import org.opennms.miniongateway.router.MinionLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RpcRequestRouterIgniteTask implements ComputeTask<RouterTaskData, byte[]> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestRouterIgniteTask.class);

    private Logger log = DEFAULT_LOGGER;

    @SpringResource(resourceName = MinionLookupService.IGNITE_SERVICE_NAME)
    private transient MinionLookupService minionLookupService;

    @IgniteInstanceResource
    private transient Ignite ignite;

    private transient Random random;

    @Override
    public @NotNull Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, @Nullable RouterTaskData arg) throws IgniteException {
        UUID gatewayNodeId = null;
        Map<ComputeJob, ClusterNode> map = new HashMap<>();
        try {
            String tenantId = arg.getTenantId();

            GatewayRpcRequestProto request = GatewayRpcRequestProto.parseFrom(arg.getRequestPayload());
            MinionIdentity target = request.getIdentity();
            if (!target.getSystemId().isBlank()) {
                gatewayNodeId = minionLookupService.findGatewayNodeWithId(tenantId, target.getSystemId());
            } else {
                gatewayNodeId = shuffle(minionLookupService.findGatewayNodeWithLocation(tenantId, target.getLocation()));
            }

            ClusterNode routingNode;
            if (gatewayNodeId == null || (routingNode = findNode(gatewayNodeId, subgrid)) == null) {
                // throw new IgniteException("Could not find active connection for location=" + request.getLocation() + " and systemId=" + request.getSystemId());
                map.put(new FailedJob(request), ignite.cluster().localNode());
            } else {
                map.put(new RoutingJob(request), routingNode);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    private <T> UUID shuffle(List<UUID> queue) {
        if (queue == null || queue.isEmpty()) {
            return null;
        }

        // generate index which is within range from [0, size).
        int index = random.nextInt(queue.size());
        return queue.get(index);
    }

    private ClusterNode findNode(UUID node, List<ClusterNode> subgrid) {
        for (ClusterNode clusterNode : subgrid) {
            if (node.equals(clusterNode.id())) {
                return clusterNode;
            }
        }
        return null;
    }

    @Override
    public ComputeJobResultPolicy result(ComputeJobResult res, List<ComputeJobResult> rcvd) throws IgniteException {
        if (rcvd.isEmpty()) {
            return ComputeJobResultPolicy.WAIT;
        }

        ComputeJobResult errorResult =
            rcvd.stream().filter((result) -> (result.getException() != null)).findFirst().orElse(null);

        if (errorResult != null) {
            throw errorResult.getException();
        }

        return ComputeJobResultPolicy.REDUCE;
    }

    @Override
    public @Nullable byte[] reduce(List<ComputeJobResult> results) throws IgniteException {
        if (results.isEmpty()) {
            return null;
        }
        ComputeJobResult jobResult = results.get(0);
        if (jobResult.getException() != null) {
            throw jobResult.getException();
        }
        return jobResult.getData();
    }

    public static class RoutingJob implements ComputeJob {

        private final GatewayRpcRequestProto request;

        @LoggerResource
        private transient IgniteLogger logger;

        @SpringResource(resourceClass = IgniteRpcRequestDispatcher.class)
        private transient IgniteRpcRequestDispatcher requestDispatcher;

        private CompletableFuture<GatewayRpcResponseProto> responseFuture;

        public RoutingJob(GatewayRpcRequestProto request) {
            this.request = request;
        }

        @Override
        public void cancel() {
            if (responseFuture != null) {
                responseFuture.cancel(true);
            }
        }

        @Override
        public byte[] execute() throws IgniteException {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Dispatching RPC request " + request);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Dispatching rpc request " + request.getRpcId());
                }
                responseFuture = requestDispatcher.execute(request).whenComplete((response, error) -> {
                    if (error != null) {
                        logger.warning("Failure found while execution of " + request.getRpcId() + " " + error);
                        return;
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Received RPC response " + response);
                    } else if (logger.isDebugEnabled()) {
                        logger.debug("Received answer for rpc request " + request.getRpcId());
                    }
                });
                return responseFuture.thenApply(AbstractMessageLite::toByteArray).get();
            } catch (InterruptedException e) {
                throw new IgniteException("Failed to dispatch request", e);
            } catch (ExecutionException e) {
                logger.warning("Failure while executing request " + request.getRpcId(), e);
                throw new IgniteException("Could not execute RPC request", e);
            }
        }
    }


    public static class FailedJob implements ComputeJob {

        private GatewayRpcRequestProto request;

        public FailedJob(GatewayRpcRequestProto request) {
            this.request = request;
        }

        @Override
        public void cancel() {

        }

        @Override
        public Object execute() throws IgniteException {
            MinionIdentity identity = request.getIdentity();
            throw new IgniteException("Could not find active connection for tenant=" + identity.getTenant() + ", location=" + identity.getLocation() + " and systemId=" + identity.getSystemId());
        }
    }
}
