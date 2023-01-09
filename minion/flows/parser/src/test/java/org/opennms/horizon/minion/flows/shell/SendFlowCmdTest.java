package org.opennms.horizon.minion.flows.shell;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class SendFlowCmdTest {
    @Test
    public void shouldSendFlow() throws Exception {
        final SendFlowCmd cmd = new SendFlowCmd();
        cmd.file = "netflow9_test_valid01.dat";
        cmd.host = "localhost";
        cmd.port = 5000;
        byte[] toSend = toByteArray(this.getClass().getResourceAsStream("/flows/" + cmd.file));

        MiniServer server = new MiniServer();
        server.start(cmd.port);
        new SendFlowCmd().execute();
        await()
            .await()
            .atMost(Duration.ofSeconds(1))
            .until(server::hasReceived);
        byte[] result = server.getBytes();
        assertArrayEquals(toSend, result);
    }

    private static class MiniServer {

        @Getter
        private byte[] bytes;
        private int port;

        private ServerSocket serverSocket;

        public void start(int port) {
            this.port = port;
            Executors.newSingleThreadExecutor()
                .submit(this::listen);
        }

        public void listen() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                log.info("ServerSocket awaiting connections...");
                final Socket socket = serverSocket.accept(); // blocking call, this will wait until a connection is attempted on this port.
                log.info("Connection from {}.", socket);
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                this.bytes = toByteArray(dataInputStream);
                log.info("Closing socket.");
                socket.close();
            } catch (IOException e) {
                log.error("an error occurred while listening.", e);
            }
        }

        public boolean hasReceived() {
            return this.bytes != null;
        }
    }

}
