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
package org.opennms.horizon.minion.flows.parser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Resources;
import com.google.protobuf.Any;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.opennms.sink.flows.contract.FlowsConfig;

public class FlowsListenerTest {

    @Test
    public void ableHandleConfig() throws IOException {
        IpcIdentity identity = mock(IpcIdentity.class);
        DnsResolver dnsResolver = mock(DnsResolver.class);

        AsyncDispatcher<FlowDocument> dispatcher = mock(AsyncDispatcher.class);
        MessageDispatcherFactory messageDispatcherFactory = mock(MessageDispatcherFactory.class);
        when(messageDispatcherFactory.createAsyncDispatcher(any(FlowSinkModule.class)))
                .thenReturn(dispatcher);
        TelemetryRegistry registry =
                new TelemetryRegistryImpl(messageDispatcherFactory, identity, dnsResolver, new MetricRegistry());

        FlowsListenerFactory manger = new FlowsListenerFactory(registry);
        final var listener = manger.create(readFlowsConfig());

        Assert.assertEquals(4, listener.getListeners().size());
        Assert.assertEquals("Netflow-5-UDP-8877", listener.getListeners().get(0).getName());
        Assert.assertEquals("Netflow-9-UDP-4729", listener.getListeners().get(1).getName());
        Assert.assertEquals("IPFIX-TCP-4730", listener.getListeners().get(2).getName());
        Assert.assertEquals("Netflow-UDP-9999", listener.getListeners().get(3).getName());

        Assert.assertEquals(3, listener.getListeners().get(3).getParsers().size());
        Assert.assertEquals(
                "Netflow-5-Parser",
                listener.getListeners().get(3).getParsers().get(0).getName());
        Assert.assertEquals(
                "Netflow-9-Parser",
                listener.getListeners().get(3).getParsers().get(1).getName());
        Assert.assertEquals(
                "IPFix-Parser",
                listener.getListeners().get(3).getParsers().get(2).getName());
    }

    Any readFlowsConfig() throws IOException {
        URL url = this.getClass().getResource("/flows-config.json");
        return Any.pack(ProtobufUtil.fromJson(Resources.toString(url, StandardCharsets.UTF_8), FlowsConfig.class));
    }
}
