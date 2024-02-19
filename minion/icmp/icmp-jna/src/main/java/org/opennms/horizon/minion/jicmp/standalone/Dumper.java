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
package org.opennms.horizon.minion.jicmp.standalone;

import com.sun.jna.Platform;
import java.net.InetAddress;
import org.opennms.horizon.minion.jicmp.ipv6.ICMPv6EchoPacket;
import org.opennms.horizon.minion.jicmp.ipv6.ICMPv6Packet.Type;
import org.opennms.horizon.minion.jicmp.jna.NativeDatagramPacket;
import org.opennms.horizon.minion.jicmp.jna.NativeDatagramSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dumper
 *
 * @author brozow
 */
public class Dumper {
    private static final Logger log = LoggerFactory.getLogger(Dumper.class);

    public void dump() throws Exception {
        NativeDatagramSocket m_pingSocket =
                NativeDatagramSocket.create(NativeDatagramSocket.PF_INET6, NativeDatagramSocket.IPPROTO_ICMPV6, 1234);

        if (Platform.isWindows()) {
            ICMPv6EchoPacket packet = new ICMPv6EchoPacket(64);
            packet.setCode(0);
            packet.setType(Type.EchoRequest);
            packet.getContentBuffer().putLong(System.nanoTime());
            packet.getContentBuffer().putLong(System.nanoTime());
            m_pingSocket.send(packet.toDatagramPacket(InetAddress.getByName("::1")));
        }

        try {
            NativeDatagramPacket datagram = new NativeDatagramPacket(65535);
            while (true) {
                m_pingSocket.receive(datagram);
                System.err.println(datagram);
            }

        } catch (Throwable e) {
            log.error("Failed to dump datagram packet", e);
        }
    }

    public static void main(String[] args) throws Exception {
        new Dumper().dump();
    }
}
