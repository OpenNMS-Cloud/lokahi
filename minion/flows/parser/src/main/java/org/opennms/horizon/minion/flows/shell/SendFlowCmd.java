package org.opennms.horizon.minion.flows.shell;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Shell command wrapper around socat to send flow data for testing purposes.
 * socat needs to be installed
 */
@Command(scope = "opennms", name = "send-flow", description = "Sends flow data for test purposes")
@Service
@SuppressWarnings("java:S106") // System.out is used intentionally: we want to see it in the Karaf shell
public class SendFlowCmd implements Action {

    @Argument()
    String host = "localhost";

    @Argument()
    int port = 5000;

    @Argument(description = "file containing the flow data")
    String file = "netflow9_test_valid01.dat";

    @Override
    public Object execute() throws Exception {
        if (file == null || file.isEmpty() || this.getClass().getResource("/flows/" + file) == null) {
           System.out.println("Please enter a valid file, e.g. 'netflow9_test_valid01.dat'");
           return null;
        }
        byte[] arr = toByteArray(this.getClass().getResourceAsStream("/flows/" + file));

        try (Socket socket = new Socket(host, port);
             final OutputStream outputStream = socket.getOutputStream();
             final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        ) {
            System.out.printf("Sending flow to the server %s:%s%n", this.host, this.port);
            dataOutputStream.write(arr);
            dataOutputStream.flush();
            dataOutputStream.close();
            System.out.println("done.");
        }
        return null;
    }
}
