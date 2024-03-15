/*
package org.test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.AvailablePortFinder;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.testng.annotations.Test;


import static org.junit.jupiter.api.Assertions.assertTrue;


public class NettyManyUDPMessagesTest extends CamelTestSupport {
    DefaultCamelContext m_camel;
    InetAddress m_host = null;
    final int SOCKET_TIMEOUT = 500;
    int m_port = 0;
    private static int serverPort=514;
    private final int messageCount = 100;
    private final String message
        = "test";



    @Test
    public void testSendingManyMessages() throws Exception {

        CamelContext camelContext = new DefaultCamelContext();

        // Add a route that receives messages from a file endpoint and logs them
        camelContext.addRoutes(new MyNettyRoute());
        camelContext.start();
        Thread.sleep(Long.MAX_VALUE); // Keep the application running


        camelContext.stop();

    }
}
*/
