/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.minion.flows.parser;

import com.google.common.io.Resources;
import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.grpc.flows.contract.FlowDocument;
import org.opennms.horizon.grpc.flows.contract.FlowDocumentLog;
import org.opennms.horizon.minion.flows.listeners.factory.TcpListenerFactory;
import org.opennms.horizon.minion.flows.listeners.factory.UdpListenerFactory;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.factory.IpfixTcpParserFactory;
import org.opennms.horizon.minion.flows.parser.factory.IpfixUdpParserFactory;
import org.opennms.horizon.minion.flows.parser.factory.Netflow5UdpParserFactory;
import org.opennms.horizon.minion.flows.parser.factory.Netflow9UdpParserFactory;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.opennms.sink.flows.contract.FlowsConfig;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigManagerTest {

    @Test
    public void ableHandleConfig() throws IOException {
        ListenerHolder holder = new ListenerHolder();
        IpcIdentity identity = mock(IpcIdentity.class);
        DnsResolver dnsResolver = mock(DnsResolver.class);

        AsyncDispatcher<FlowDocumentLog> dispatcher = mock(AsyncDispatcher.class);
        MessageDispatcherFactory messageDispatcherFactory = mock(MessageDispatcherFactory.class);
        when(messageDispatcherFactory.createAsyncDispatcher(any(FlowSinkModule.class))).thenReturn(dispatcher);
        TelemetryRegistry registry = new TelemetryRegistryImpl(messageDispatcherFactory, identity, dnsResolver, holder);

        UdpListenerFactory udpFactory = new UdpListenerFactory(registry);
        TcpListenerFactory tcpFactory = new TcpListenerFactory(registry);
        Netflow5UdpParserFactory netflow5UdpParserFactory = new Netflow5UdpParserFactory(registry, identity, dnsResolver);
        Netflow9UdpParserFactory netflow9UdpParserFactory = new Netflow9UdpParserFactory(registry, identity, dnsResolver);
        IpfixTcpParserFactory ipfixTcpParserFactory = new IpfixTcpParserFactory(registry, identity, dnsResolver);
        IpfixUdpParserFactory ipfixUdpParserFactory = new IpfixUdpParserFactory(registry, identity, dnsResolver);

        ConfigManager manger = new ConfigManager(registry);
        manger.create(readFlowsConfig());

        Assert.assertEquals(4, holder.size());
        Assert.assertNotNull(holder.get("IPFIX-TCP-4730"));
        Assert.assertNotNull(holder.get("Netflow-5-UDP-8877"));
        Assert.assertNotNull(holder.get("Netflow-9-UDP-4729"));
        Assert.assertNotNull(holder.get("Netflow-UDP-9999"));
    }

    Any readFlowsConfig() throws IOException {
        URL url = this.getClass().getResource("/flows-config.json");
        return Any.pack(ProtobufUtil.fromJson(Resources.toString(url, StandardCharsets.UTF_8), FlowsConfig.class));
    }
}
