/*
package org.test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.netty.NettyComponent;
import org.apache.camel.component.netty.NettyConstants;
import org.apache.camel.component.syslog.SyslogDataFormat;
import org.apache.camel.component.syslog.SyslogMessage;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultManagementNameStrategy;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.AvailablePortFinder;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.shared.utils.InetAddressUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyManyUDPMessagesTest extends CamelTestSupport {
    DefaultCamelContext m_camel;
    InetAddress m_host = null;
    final int SOCKET_TIMEOUT = 500;
    int m_port = 0;
    private static int serverPort=514;
    private final int messageCount = 100;
    private final String message
        = "<165>Aug  4 05:34:00 mymachine myproc[10]: %% It's\n         time to make the do-nuts.  %%  Ingredients: Mix=OK, Jelly=OK #\n"
        + "         Devices: Mixer=OK, Jelly_Injector=OK, Frier=OK # Transport:\n"
        + "         Conveyer1=OK, Conveyer2=OK # %%";



    @Test
    public void testSendingManyMessages() throws Exception {

        CamelContext camelContext = new DefaultCamelContext();

        // Add a route that receives messages from a file endpoint and logs them
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .setBody(constant("Hello from Camel!")) // Set the message body
                    .to("http://www.google.com"); // URL to send the message
            }
        });
        boolean isHttpRegistered = camelContext.getComponentNames().contains("http");
        List<String> components = camelContext.getComponentNames();
        for(String str : components) {
            System.out.println(str);
        }
        // Start Camel context
        camelContext.start();

        // Keep the application running until terminated
      //  Thread.sleep(Long.MAX_VALUE);

        // Stop Camel context
        camelContext.stop();

    }
}
*/
