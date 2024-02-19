/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.icmp.shell;

import java.net.InetAddress;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.shared.icmp.PingConstants;

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

    @Argument(
            index = 0,
            name = "begin",
            description = "First address of the IP range to be pinged",
            required = true,
            multiValued = false)
    String m_begin;

    @Argument(
            index = 1,
            name = "end",
            description = "Last address of the IP range to be pinged",
            required = true,
            multiValued = false)
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
