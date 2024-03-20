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
package org.opennms.horizon.minion.syslog.listener;




import io.netty.buffer.ByteBuf;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.NettyComponent;
import org.apache.camel.component.netty.NettyConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultManagementNameStrategy;
import org.apache.camel.support.SimpleRegistry;
import org.opennms.horizon.shared.utils.InetAddressUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.opennms.horizon.shared.utils.InetAddressUtils.addr;


/**
 * @author Seth
 */
public class SyslogReceiverCamelNettyImpl extends SinkDispatchingSyslogReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogReceiverCamelNettyImpl.class);

    private static final int SOCKET_TIMEOUT = 500;

    private InetAddress m_host;

    private int m_port;

    private final SyslogdConfig m_config;

    private DefaultCamelContext m_camel;

    public SyslogReceiverCamelNettyImpl(final SyslogdConfig config) {
        super(config);
        m_config = config;
        setHostAndPort();
    }

    @Override
    public String getName() {
        String listenAddress = m_config.getListenAddress() == null? "0.0.0.0" : m_config.getListenAddress();
        return getClass().getSimpleName() + " [" + listenAddress + ":" + m_config.getSyslogPort() + "]";
    }

    public boolean isStarted() {
        if (m_camel == null) {
            return false;
        } else {
            return m_camel.isStarted();
        }
    }

    /**
     * stop the current receiver
     * @throws InterruptedException
     */
    @Override
    public void stop() throws InterruptedException {
        try {
            if (m_camel != null) {
                m_camel.shutdown();
            }
        } catch (Exception e) {
            LOG.warn("Exception while shutting down syslog Camel context", e);
        }
        super.stop();
    }

    /**
     * The execution context.
     */
    @Override
    public void run() {
        // Setup logging and create the dispatcher
        super.run();

        SimpleRegistry registry = new SimpleRegistry();

        //Adding netty component to camel in order to resolve OSGi loading issues
        NettyComponent nettyComponent = new NettyComponent();
        m_camel = new DefaultCamelContext(registry);

        // Set the context name so that it shows up nicely in JMX
        //
        // @see org.apache.camel.management.DefaultManagementNamingStrategy
        //
        //m_camel.setManagementName("org.opennms.features.events.syslog.listener");
        m_camel.setName("syslogdListenerCamelNettyContext");
        m_camel.setManagementNameStrategy(new DefaultManagementNameStrategy(m_camel, "#name#", null));

        m_camel.addComponent("netty", nettyComponent);

        m_camel.getShutdownStrategy().setShutdownNowOnTimeout(true);
        m_camel.getShutdownStrategy().setTimeout(15);
        m_camel.getShutdownStrategy().setTimeUnit(TimeUnit.SECONDS);

        try {
            m_camel.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    String from = String.format("netty:udp://%s:%d?sync=false&allowDefaultCodec=false&receiveBufferSize=%d&connectTimeout=%d",
                        InetAddressUtils.str(m_host),
                        m_port,
                        Integer.MAX_VALUE,
                        SOCKET_TIMEOUT
                    );
                    from(from)
                    // Polled via JMX
                    .routeId("syslogListen")
                    .process(new AsyncProcessor() {

                        @Override
                        public void process(Exchange exchange) throws Exception {
                            final ByteBuf buffer = exchange.getIn().getBody(ByteBuf.class);

                            // NettyConstants.NETTY_REMOTE_ADDRESS is a SocketAddress type but because
                            // we are listening on an InetAddress, it will always be of type InetAddressSocket
                            InetSocketAddress source = (InetSocketAddress)exchange.getIn().getHeader(NettyConstants.NETTY_REMOTE_ADDRESS);

                          System.out.println(source.getHostName());
                        }

                        @Override
                        public boolean process(Exchange exchange, AsyncCallback callback) {
                            final ByteBuf buffer = exchange.getIn().getBody(ByteBuf.class);

                            // NettyConstants.NETTY_REMOTE_ADDRESS is a SocketAddress type but because
                            // we are listening on an InetAddress, it will always be of type InetAddressSocket
                            InetSocketAddress source = (InetSocketAddress)exchange.getIn().getHeader(NettyConstants.NETTY_REMOTE_ADDRESS);

                            ByteBuffer bufferCopy = ByteBuffer.allocate(buffer.readableBytes());
                            buffer.getBytes(buffer.readerIndex(), bufferCopy);

                            System.out.println(source.getHostName());
                            return false;
                        }

                        @Override
                        public CompletableFuture<Exchange> processAsync(Exchange exchange) {
                            final ByteBuf buffer = exchange.getIn().getBody(ByteBuf.class);

                            // NettyConstants.NETTY_REMOTE_ADDRESS is a SocketAddress type but because
                            // we are listening on an InetAddress, it will always be of type InetAddressSocket
                            InetSocketAddress source = (InetSocketAddress)exchange.getIn().getHeader(NettyConstants.NETTY_REMOTE_ADDRESS);

                            System.out.println(source.getHostName() +source.getPort());
                            return null;
                        }


                    });
                }
            });
            m_camel.start();
            Thread.sleep(Long.MAX_VALUE);
        } catch (Throwable e) {
            LOG.error("Could not configure Camel routes for syslog receiver", e);
        }
    }

    private void setHostAndPort() {
        m_host = addr(m_config.getListenAddress() == null? "0.0.0.0" : m_config.getListenAddress());
        m_port = m_config.getSyslogPort();
    }

    @Override
    public void reload() throws IOException {
        m_config.reload();
        setHostAndPort();
    }
}
