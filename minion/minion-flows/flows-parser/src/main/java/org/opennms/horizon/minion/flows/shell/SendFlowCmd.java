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
package org.opennms.horizon.minion.flows.shell;

import static com.google.common.io.ByteStreams.toByteArray;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.Setter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 * Shell command to send flow data for testing purposes.
 */
@Command(scope = "opennms", name = "send-flow", description = "Sends flow data for test purposes")
@Service
@Setter
@SuppressWarnings("java:S106") // System.out is used intentionally: we want to see it in the Karaf shell
public class SendFlowCmd implements Action {

    @Option(name = "-h", aliases = "--host", description = "host to send to, default: localhost")
    String host = "localhost";

    @Option(name = "-p", aliases = "--port", description = "port to send to, default: 50000")
    int port = 50000;

    @Option(
            name = "-f",
            aliases = "--file",
            description = "file containing flow data, default: netflow9_test_valid01.dat")
    String file = "netflow9_test_valid01.dat";

    @Override
    public Object execute() throws Exception {

        byte[] dataToSend;
        if (Files.exists(Paths.get(file))) {
            dataToSend = Files.readAllBytes(Paths.get(file));
        } else if (this.getClass().getResource("/flows/" + file) != null) {
            dataToSend = toByteArray(this.getClass().getResourceAsStream("/flows/" + file));
        } else {
            System.out.printf(
                    "Can not read file %s. Please enter a valid file, e.g. 'netflow9_test_valid01.dat'.%n", file);
            return null;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            System.out.printf("Sending flow to the server %s:%s%n", this.host, this.port);
            InetAddress ip = InetAddress.getByName(host);
            DatagramPacket dp = new DatagramPacket(dataToSend, dataToSend.length, ip, port);
            socket.send(dp);
            System.out.println("done.");
        }
        return null;
    }
}
