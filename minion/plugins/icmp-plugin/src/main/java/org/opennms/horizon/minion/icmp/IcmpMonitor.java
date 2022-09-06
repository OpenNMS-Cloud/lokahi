
package org.opennms.horizon.minion.icmp;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.opennms.echo.contract.EchoRequest;
import org.opennms.netmgt.icmp.EchoPacket;
import org.opennms.netmgt.icmp.PingConstants;
import org.opennms.netmgt.icmp.PingResponseCallback;
import org.opennms.netmgt.icmp.Pinger;
import org.opennms.netmgt.icmp.PingerFactory;
import org.opennms.horizon.minion.plugin.api.AbstractServiceMonitor;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl;

@RequiredArgsConstructor
public class IcmpMonitor extends AbstractServiceMonitor {

    @Setter
    private String moreConfig;

    private final PingerFactory pingerFactory;

    @Override
    public CompletableFuture<ServiceMonitorResponse> poll(MonitoredService svc, Any config) {

        CompletableFuture<ServiceMonitorResponse> future = new CompletableFuture<>();

        try {
            if (! config.is(EchoRequest.class)) {
                throw new IllegalArgumentException("configuration must be an EchoRequest; type-url=" + config.getTypeUrl());
            }

            // TBD888: add all settings to EchoRequest.  Change the pingers to directly use EchoRequest?
            EchoRequest echoRequest = config.unpack(EchoRequest.class);

            String hostString = echoRequest.getHost();
            InetAddress host = InetAddress.getByName(hostString);

            long timeout = (long) echoRequest.getTimeout();

            // int retries = ParameterMap.getKeyedInteger(config, "retry", PingConstants.DEFAULT_RETRIES);
            int retries = PingConstants.DEFAULT_RETRIES;

            // int packetSize = ParameterMap.getKeyedInteger(config, "packet-size", PingConstants.DEFAULT_PACKET_SIZE);
            int packetSize = PingConstants.DEFAULT_PACKET_SIZE;
            // int dscp = ParameterMap.getKeyedDecodedInteger(config, "dscp", 0);

            int dscp = 0;
            // boolean allowFragmentation = ParameterMap.getKeyedBoolean(config, "allow-fragmentation", true);
            boolean allowFragmentation = true;

            Pinger pinger = pingerFactory.getInstance(dscp, allowFragmentation);

            pinger.ping(host, timeout, retries, packetSize, new PingResponseCallback() {
                @Override
                public void handleResponse(InetAddress inetAddress, EchoPacket response) {
                    double responseTimeMicros = Math.round(response.elapsedTime(TimeUnit.MICROSECONDS));

                    future.complete(
                        ServiceMonitorResponseImpl.builder()
                            .status(Status.Up)
                            .responseTime(responseTimeMicros)
                            .ipAddress(inetAddress.getHostAddress())
                            .build()
                    );
                }

                @Override
                public void handleTimeout(InetAddress inetAddress, EchoPacket echoPacket) {
                    future.complete(ServiceMonitorResponseImpl.unknown());
                }

                @Override
                public void handleError(InetAddress inetAddress, EchoPacket echoPacket, Throwable throwable) {
                    future.complete(ServiceMonitorResponseImpl.down());
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}
