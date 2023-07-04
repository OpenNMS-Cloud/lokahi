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

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Resources;
import com.google.protobuf.Any;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
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

public class FlowsListenerTest {

    @Test
    public void ableHandleConfig() throws IOException {
        IpcIdentity identity = mock(IpcIdentity.class);
        DnsResolver dnsResolver = mock(DnsResolver.class);

        AsyncDispatcher<FlowDocument> dispatcher = mock(AsyncDispatcher.class);
        MessageDispatcherFactory messageDispatcherFactory = mock(MessageDispatcherFactory.class);
        when(messageDispatcherFactory.createAsyncDispatcher(any(FlowSinkModule.class))).thenReturn(dispatcher);
        TelemetryRegistry registry = new TelemetryRegistryImpl(messageDispatcherFactory, identity, dnsResolver, new MetricRegistry());

        FlowsListenerFactory manger = new FlowsListenerFactory(registry);
        final var listener = manger.create(readFlowsConfig());

        Assert.assertEquals(4, listener.getListeners().size());
        Assert.assertEquals("Netflow-5-UDP-8877", listener.getListeners().get(0).getName());
        Assert.assertEquals("Netflow-9-UDP-4729", listener.getListeners().get(1).getName());
        Assert.assertEquals("IPFIX-TCP-4730", listener.getListeners().get(2).getName());
        Assert.assertEquals("Netflow-UDP-9999", listener.getListeners().get(3).getName());

        Assert.assertEquals(3, listener.getListeners().get(3).getParsers().size());
        Assert.assertEquals("Netflow-5-Parser", listener.getListeners().get(3).getParsers().get(0).getName());
        Assert.assertEquals("Netflow-9-Parser", listener.getListeners().get(3).getParsers().get(1).getName());
        Assert.assertEquals("IPFix-Parser", listener.getListeners().get(3).getParsers().get(2).getName());
    }

    Any readFlowsConfig() throws IOException {
        URL url = this.getClass().getResource("/flows-config.json");
        return Any.pack(ProtobufUtil.fromJson(Resources.toString(url, StandardCharsets.UTF_8), FlowsConfig.class));
    }
}
