package org.opennms.horizon.minion.plugin.api;


import java.io.IOException;
import java.net.Socket;

public class DefaultSocketWrapper implements SocketWrapper {
    @Override
    public Socket wrapSocket(Socket socket) throws IOException {
        return socket;
    }
}
