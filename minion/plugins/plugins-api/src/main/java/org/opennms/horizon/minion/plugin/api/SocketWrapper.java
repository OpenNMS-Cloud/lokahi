package org.opennms.horizon.minion.plugin.api;

import java.io.IOException;
import java.net.Socket;

public interface SocketWrapper {
    Socket wrapSocket(Socket socket) throws IOException;
}
