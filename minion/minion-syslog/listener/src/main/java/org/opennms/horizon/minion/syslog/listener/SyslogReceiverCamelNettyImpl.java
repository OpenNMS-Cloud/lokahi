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


        try {
            DefaultCamelContext camelContext = new DefaultCamelContext();
            camelContext.addRoutes(new MyCamelRoute());
            camelContext.start();

            // Keep the JVM running to keep the routes active
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
