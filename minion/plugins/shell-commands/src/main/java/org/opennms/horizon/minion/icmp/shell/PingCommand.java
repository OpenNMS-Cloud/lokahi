/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.minion.icmp.shell;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.shared.icmp.PingConstants;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.icmp.contract.IpRange;
import org.opennms.icmp.contract.PingSweepRequest;
import org.opennms.taskset.contract.DiscoveryScanResult;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Command(scope = "opennms", name = "ping", description = "Ping a given Ip Address")
@Service
public class PingCommand implements Action {

    @Reference
    private ScannerRegistry scannerRegistry;

    @Option(name = "-r", aliases = "--retries", description = "Number of retries")
    int retries = PingConstants.DEFAULT_RETRIES;

    @Option(name = "-t", aliases = "--timeout", description = "Timeout in milliseconds")
    int timeout = PingConstants.DEFAULT_TIMEOUT;

    @Option(name = "-p", aliases = "--packetsize", description = "Packet size")
    int packetsize = PingConstants.DEFAULT_PACKET_SIZE;

    @Option(name = "--pps", description = "packets per second")
    double packetsPerSecond = 100;

    @Argument(index = 0, name = "ipAddress", description = "Ip address to be pinged", required = true, multiValued = false)
    String ipAddress;



    @Override
    public Object execute() throws Exception {

        final InetAddress begin = InetAddress.getByName(ipAddress);
        final InetAddress end = InetAddress.getByName(ipAddress);

        System.out.printf("Pinging host %s :\n", begin.getHostAddress());
        System.out.printf("\tRetries: %d\n", retries);
        System.out.printf("\tTimeout: %d\n", timeout);
        System.out.printf("\tPacket size: %d\n", packetsize);
        System.out.printf("\tPackets per second: %f\n", packetsPerSecond);
        ping(scannerRegistry, begin, end, retries, timeout, packetsize, packetsPerSecond);
        return null;
    }

    static void ping(ScannerRegistry scannerRegistry, InetAddress begin, InetAddress end,
                     int retries, int timeout, int packetsize,
                     double packetsPerSecond) {
        long startTime = System.currentTimeMillis();

        var scannerManager = scannerRegistry.getService("Discovery-Ping");
        var scanner = scannerManager.create();
        var ipRange = IpRange.newBuilder().setBegin(InetAddressUtils.str(begin))
            .setEnd(InetAddressUtils.str(end)).build();
        Any configuration = Any.pack(PingSweepRequest.newBuilder()
            .addIpRange(ipRange)
            .setRetries(retries)
            .setTimeout(timeout)
            .setPacketSize(packetsize)
            .setPacketsPerSecond(packetsPerSecond)
            .build());
        var future = scanner.scan(configuration);

        while (true) {
            try {
                try {
                    var response = future.get(1, TimeUnit.SECONDS);
                    Any result = Any.pack(response.getResults());
                    var pingResults = result.unpack(DiscoveryScanResult.class);

                    if (pingResults.getPingResponseList().isEmpty()) {
                        System.out.print("\n\nNone of the IP addresses responsed to our pings.\n");
                    } else {
                        System.out.print("\n\nIP Address\tRound-trip time\n");
                        pingResults.getPingResponseList().forEach((pingResponse) -> {
                            System.out.printf("%s\t%.3f ms\n", pingResponse.getIpAddress(), pingResponse.getRtt());
                        });
                    }
                    long endTime = System.currentTimeMillis();
                    System.out.printf("Total time took to finish the Ping sweep: %d secs", (endTime - startTime)/1000);
                } catch (InterruptedException e) {
                    System.out.println("\n\nInterrupted.");
                } catch (ExecutionException e) {
                    System.out.printf("\n\nPing Sweep failed with: %s\n", e);
                } catch (InvalidProtocolBufferException e) {
                    System.out.printf("Invalid proto %s", e);
                }
                break;
            } catch (TimeoutException e) {
                // pass
            }
            System.out.print(".");
        }
    }
}
