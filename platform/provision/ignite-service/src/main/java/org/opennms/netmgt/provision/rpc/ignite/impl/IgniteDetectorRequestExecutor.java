package org.opennms.netmgt.provision.rpc.ignite.impl;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentracing.Span;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.IgniteClientFuture;
import org.jetbrains.annotations.NotNull;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.Identity.Builder;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.grpc.detector.contract.Attribute;
import org.opennms.horizon.grpc.detector.contract.DetectorRequest;
import org.opennms.horizon.grpc.detector.contract.DetectorResponse;
import org.opennms.netmgt.provision.DetectorRequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IgniteDetectorRequestExecutor implements DetectorRequestExecutor {

    private final Logger logger = LoggerFactory.getLogger(IgniteDetectorRequestExecutor.class);

    private final IgniteClient igniteClient;
    private final String location;
    private final String systemId;
    private final String serviceName;
    private final String detectorName;
    private final InetAddress address;
    private final Map<String, String> attributes;
    private final Map<String, String> runtimeAttributes;
    private final Integer nodeId;
    private final Span span; // TODO: wire into the remote call


    public IgniteDetectorRequestExecutor(
        IgniteClient igniteClient,
        String location,
        String systemId,
        String serviceName,
        String detectorName,
        InetAddress address,
        Map<String, String> attributes,
        Map<String, String> runtimeAttributes,
        Integer nodeId,
        Span span
    ) {

        this.igniteClient = igniteClient;
        this.location = location;
        this.systemId = systemId;
        this.serviceName = serviceName;
        this.detectorName = detectorName;
        this.address = address;
        this.attributes = attributes;
        this.runtimeAttributes = runtimeAttributes;
        this.nodeId = nodeId;
        this.span = span;
    }

    @Override
    public CompletableFuture<Boolean> execute() {
        Builder identityBuilder = Identity.newBuilder().setLocation(location);
        Optional.ofNullable(systemId).ifPresent(identityBuilder::setSystemId);

        // TODO make sure all required parameters are passed
        DetectorRequest requestObj = DetectorRequest.newBuilder()
            .setIdentity(identityBuilder.build())
            .addAllDetectorAttributes(mapAttributes(attributes))
            .addAllRuntimeAttributes(mapAttributes(runtimeAttributes))
            .setClassName(Optional.ofNullable(detectorName).orElse(""))
            .setAddress(Optional.ofNullable(address).map(Object::toString).orElse(""))
            .build();

        RpcRequestProto.Builder rpcRequest = RpcRequestProto.newBuilder()
            .setLocation(location)
            .setModuleId("detector")
            .setExpirationTime(System.currentTimeMillis() + 10_000)
            .setPayload(Any.pack(requestObj))
            .setRpcId(UUID.randomUUID().toString());

        Optional.ofNullable(systemId).ifPresent(rpcRequest::setSystemId);

        IgniteClientFuture<RpcRequestProto> dispatcher = null;
//            .executeAsync2(MinionLookupService.IGNITE_SERVICE_NAME, rpcRequest.build());

        return dispatcher.toCompletableFuture()
            .thenApply(RpcRequestProto::getPayload)
            .thenApply(any -> {
                try {
                    return any.unpack(DetectorResponse.class);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(DetectorResponse::getDetected);
    }

    @NotNull
    private List<Attribute> mapAttributes(Map<String, String> attributes) {
        return Optional.ofNullable(attributes)
            .map(Map::entrySet)
            .map(entries -> entries.stream()
                .map(this::createAttribute)
                .collect(Collectors.toList())
            ).orElse(Collections.emptyList());
    }

    @NotNull
    private Attribute createAttribute(Entry<String, String> entry) {
        return Attribute.newBuilder()
            .setKey(entry.getKey())
            .setValue(entry.getValue())
            .build();
    }

}
