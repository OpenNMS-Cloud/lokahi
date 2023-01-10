package org.opennms.horizon.minion.flows.shell;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.Duration;
import java.util.concurrent.Executors;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;

@Slf4j
public class SendFlowCmdTest {


    private final static String TEST_FILE = "netflow9_test_valid01.dat";
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
        await()
            .await()
            .atMost(Duration.ofSeconds(1))
            .until(() -> server.getPort() > 0); // wait until we have an assigned port
        cmd.port = server.getPort();

        // send package
        cmd.execute();
        await()
            .await()
            .atMost(Duration.ofSeconds(1))
            .until(server::hasReceived);// wait until the packet was received

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
            Executors.newSingleThreadExecutor()
                .submit(this::listen);
        }

        public void listen() {
            try (DatagramSocket serverSocket = new DatagramSocket()) {
                this.port = serverSocket.getLocalPort();
                log.info("ServerSocket awaiting connections...");
                byte[] buf = new byte[packetLength];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet); // blocking call, this will wait until a connection is attempted on this port.
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
