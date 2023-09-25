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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.shared.icmp.PingConstants;

import java.net.InetAddress;

@Command(scope = "opennms", name = "ping-sweep", description = "Ping Sweep for a given Ip range")
@Service
public class PingSweepCommand implements Action {

    @Reference
    private ScannerRegistry scannerRegistry;

    @Option(name = "-r", aliases = "--retries", description = "Number of retries")
    int retries = PingConstants.DEFAULT_RETRIES;

    @Option(name = "-t", aliases = "--timeout", description = "Timeout in milliseconds")
    int timeout = PingConstants.DEFAULT_TIMEOUT;

    @Option(name = "-p", aliases = "--packetsize", description = "Packet size")
    int packetsize = PingConstants.DEFAULT_PACKET_SIZE;

    @Option(name = "--pps", description = "packets per second")
    double packetsPerSecond = PingConstants.DEFAULT_PACKETS_PER_SECOND;

    @Argument(index = 0, name = "begin", description = "First address of the IP range to be pinged", required = true, multiValued = false)
    String m_begin;

    @Argument(index = 1, name = "end", description = "Last address of the IP range to be pinged", required = true, multiValued = false)
    String m_end;


    @Override
    public Object execute() throws Exception {

        var scannerManager = scannerRegistry.getService("Discovery-Ping");
        var scanner = scannerManager.create();

        final InetAddress begin = InetAddress.getByName(m_begin);
        final InetAddress end = InetAddress.getByName(m_end);

        System.out.printf("Pinging hosts from %s to %s with:\n", begin.getHostAddress(), end.getHostAddress());
        System.out.printf("\tRetries: %d\n", retries);
        System.out.printf("\tTimeout: %d\n", timeout);
        System.out.printf("\tPacket size: %d\n", packetsize);
        System.out.printf("\tPackets per second: %f\n", packetsPerSecond);
        PingCommand.ping(scannerRegistry, begin, end, retries, timeout, packetsize, packetsPerSecond);

        return null;
    }
}
