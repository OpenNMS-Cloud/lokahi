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
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Duration;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class SendFlowCmdTest {

    private static final String TEST_FILE = "netflow9_test_valid01.dat";

    @Test
    public void shouldSendFlow() throws Exception {

        // create test server
        byte[] toSend = toByteArray(this.getClass().getResourceAsStream("/flows/" + TEST_FILE));
        MiniServer server = new MiniServer(toSend.length);
        server.start();

        // create command
        final SendFlowCmd cmd = new SendFlowCmd();
        cmd.file = TEST_FILE;
        cmd.host = "localhost";
        await().await()
                .atMost(Duration.ofSeconds(1))
                .until(() -> server.getPort() > 0); // wait until we have an assigned port
        cmd.port = server.getPort();

        // send package
        cmd.execute();
        await().await().atMost(Duration.ofSeconds(1)).until(server::hasReceived); // wait until the packet was received

        // check
        byte[] result = server.getBytes();
        assertArrayEquals(toSend, result);
    }

    @RequiredArgsConstructor
    private static class MiniServer {

        private final int packetLength;

        @Getter
        private byte[] bytes;

        @Getter
        private int port;

        public void start() {
            Executors.newSingleThreadExecutor().submit(this::listen);
        }

        public void listen() {
            try (DatagramSocket serverSocket = new DatagramSocket()) {
                this.port = serverSocket.getLocalPort();
                log.info("ServerSocket awaiting connections...");
                byte[] buf = new byte[packetLength];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(
                        packet); // blocking call, this will wait until a connection is attempted on this port.
                this.bytes = packet.getData();
                log.info("Connection from {}.", packet.getAddress());
            } catch (IOException e) {
                log.error("an error occurred while listening.", e);
            }
        }

        public boolean hasReceived() {
            return this.bytes != null;
        }
    }
}
