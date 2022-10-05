package org.opennms.horizon.minion.icmp;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import org.opennms.horizon.shared.icmp.EchoPacket;
import org.opennms.horizon.shared.icmp.PingConstants;
import org.opennms.horizon.shared.icmp.PingResponseCallback;
import org.opennms.horizon.shared.icmp.Pinger;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.horizon.minion.plugin.api.AbstractServiceMonitor;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcmpMonitor extends AbstractServiceMonitor {

    private final PingerFactory pingerFactory;

    private final Descriptors.FieldDescriptor allowFragmentationFieldDescriptor;
    private final Descriptors.FieldDescriptor dscpFieldDescriptor;
    private final Descriptors.FieldDescriptor hostFieldDescriptor;
    private final Descriptors.FieldDescriptor packetSizeFieldDescriptor;
    private final Descriptors.FieldDescriptor retriesFieldDescriptor;
    private final Descriptors.FieldDescriptor timeoutFieldDescriptor;

    public IcmpMonitor(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;

        Descriptors.Descriptor echoMonitorRequestDescriptor = IcmpMonitorRequest.getDefaultInstance().getDescriptorForType();

        allowFragmentationFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.ALLOW_FRAGMENTATION_FIELD_NUMBER);
        dscpFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.DSCP_FIELD_NUMBER);
        hostFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.HOST_FIELD_NUMBER);
        packetSizeFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.PACKET_SIZE_FIELD_NUMBER);
        retriesFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.RETRIES_FIELD_NUMBER);
        timeoutFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.TIMEOUT_FIELD_NUMBER);
    }

//========================================
//
//----------------------------------------

    @Override
    public CompletableFuture<ServiceMonitorResponse> poll(MonitoredService svc, Any config) {

        CompletableFuture<ServiceMonitorResponse> future = new CompletableFuture<>();

        try {
            if (! config.is(IcmpMonitorRequest.class)) {
                throw new IllegalArgumentException("configuration must be an EchoRequest; type-url=" + config.getTypeUrl());
            }

            IcmpMonitorRequest IcmpMonitorRequest = config.unpack(IcmpMonitorRequest.class);
            IcmpMonitorRequest effectiveRequest = populateDefaultsAsNeeded(IcmpMonitorRequest);

            String hostString = effectiveRequest.getHost();
            InetAddress host = InetAddress.getByName(hostString);

            boolean allowFragmentation = effectiveRequest.getAllowFragmentation();

            Pinger pinger = pingerFactory.getInstance(effectiveRequest.getDscp(), allowFragmentation);

            pinger.ping(
                host,
                effectiveRequest.getTimeout(),
                effectiveRequest.getRetries(),
                effectiveRequest.getPacketSize(),
                new MyPingResponseCallback(future)
            );
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

//========================================
// Internal Methods
//----------------------------------------

    private IcmpMonitorRequest populateDefaultsAsNeeded(IcmpMonitorRequest IcmpMonitorRequest) {
        IcmpMonitorRequest.Builder resultBuilder = IcmpMonitorRequest.newBuilder(IcmpMonitorRequest);

        if (! IcmpMonitorRequest.hasField(retriesFieldDescriptor)) {
            resultBuilder.setRetries(PingConstants.DEFAULT_RETRIES);
        }

        if ((! IcmpMonitorRequest.hasField(packetSizeFieldDescriptor)) || (IcmpMonitorRequest.getPacketSize() <= 0)) {
            resultBuilder.setPacketSize(PingConstants.DEFAULT_PACKET_SIZE);
        }

        if (! IcmpMonitorRequest.hasField(dscpFieldDescriptor)) {
            resultBuilder.setDscp(0);
        }

        if (! IcmpMonitorRequest.hasField(allowFragmentationFieldDescriptor)) {
            resultBuilder.setAllowFragmentation(true);
        }

        if (! IcmpMonitorRequest.hasField(timeoutFieldDescriptor)) {
            resultBuilder.setTimeout(PingConstants.DEFAULT_TIMEOUT);
        }

        return resultBuilder.build();
    }


//========================================
// Internal Classes
//----------------------------------------

    private static class MyPingResponseCallback implements PingResponseCallback {
        private final Logger logger = LoggerFactory.getLogger(MyPingResponseCallback.class);
        private final CompletableFuture<ServiceMonitorResponse> future;

        public MyPingResponseCallback(CompletableFuture<ServiceMonitorResponse> future) {
            this.future = future;
        }

        @Override
        public void handleResponse(InetAddress inetAddress, EchoPacket response) {
            double responseTimeMicros = Math.round(response.elapsedTime(TimeUnit.MICROSECONDS));
            double responseTimeMillis = responseTimeMicros / 1000.0;

            future.complete(
                ServiceMonitorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .status(Status.Up)
                    .responseTime(responseTimeMillis)
                    .ipAddress(inetAddress.getHostAddress())
                    .build()
            );
        }

        @Override
        public void handleTimeout(InetAddress inetAddress, EchoPacket echoPacket) {
            future.complete(
                ServiceMonitorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .status(Status.Unknown)
                    .ipAddress(inetAddress.getHostAddress())
                    .build()
            );
        }

        @Override
        public void handleError(InetAddress inetAddress, EchoPacket echoPacket, Throwable throwable) {
            future.complete(
                ServiceMonitorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .status(Status.Down)
                    .ipAddress(inetAddress.getHostAddress())
                    .build()
            );
        }
    }
}
