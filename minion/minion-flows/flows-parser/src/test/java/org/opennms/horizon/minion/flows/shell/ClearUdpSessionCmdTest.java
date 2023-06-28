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

package org.opennms.horizon.minion.flows.shell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.listeners.Listener;
import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.parser.FlowSinkModule;
import org.opennms.horizon.minion.flows.parser.FlowsListenerFactory;
import org.opennms.horizon.minion.flows.parser.TelemetryRegistry;
import org.opennms.horizon.minion.flows.parser.TelemetryRegistryImpl;
import org.opennms.horizon.minion.flows.parser.TestUtil;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.session.SequenceNumberTracker;
import org.opennms.horizon.minion.flows.parser.session.UdpSessionManager;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.opennms.sink.flows.contract.FlowsConfig;

import com.google.common.io.Resources;
import com.google.protobuf.Any;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClearUdpSessionCmdTest {

    private final static String TEST_FILE = "/flows/netflow9_test_cisco_wlc_tpl.dat";

    @Test
    public void udpSessionIsRetrievedAndDroppedSuccessfully() throws Exception {
        // Given
        IpcIdentity identity = mock(IpcIdentity.class);
        DnsResolver dnsResolver = mock(DnsResolver.class);
        new UdpSessionManager(Duration.ofMinutes(30), () -> new SequenceNumberTracker(32));

        AsyncDispatcher<FlowDocument> dispatcher = mock(AsyncDispatcher.class);
        MessageDispatcherFactory messageDispatcherFactory = mock(MessageDispatcherFactory.class);
        when(messageDispatcherFactory.createAsyncDispatcher(any(FlowSinkModule.class))).thenReturn(dispatcher);
        TelemetryRegistry telemetryRegistry = new TelemetryRegistryImpl(messageDispatcherFactory, identity, dnsResolver, new MetricRegistry());

        FlowsListenerFactory.FlowsListener flowsListener = new FlowsListenerFactory(telemetryRegistry).create(readFlowsConfig());

        // Set up ClearSeassionCmd parameters
        final ClearUdpSessionCmd clearSessionCmd = new ClearUdpSessionCmd();
        clearSessionCmd.parserName = "Netflow-9-Parser";
        clearSessionCmd.observationDomainId = 1;
        clearSessionCmd.flowsListener = flowsListener;

        ScheduledExecutorService scheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
        InetSocketAddress localSocketAddress = new InetSocketAddress("localhost", 49152);
        InetSocketAddress remoteSocketAddress = buildLocalSocketAddress(TestUtil.findAvailablePort(12345, 12370));

        Optional<? extends Parser> matchedParser = flowsListener.getListeners().stream()
            .flatMap(listener -> listener.getParsers().stream())
            .filter(UdpParser.class::isInstance)
            .filter(parser -> clearSessionCmd.parserName.equals(parser.getName()))
            .findFirst();

        execute(TEST_FILE, buffer -> {
            try {
                matchedParser.get().start(scheduledExecutorService);
                ((UdpParser) matchedParser.get()).parse(buffer, remoteSocketAddress, localSocketAddress);
                assertFalse(((UdpParser) matchedParser.get()).getSessionManager().getTemplates().isEmpty());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        // When
        try {
            clearSessionCmd.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Then
        assertTrue(((UdpParser) matchedParser.get()).getSessionManager().getTemplates().isEmpty());

    }

    private InetSocketAddress buildLocalSocketAddress(int port) {
        return new InetSocketAddress("localhost", port);
    }

    Any readFlowsConfig() throws IOException {
        URL url = this.getClass().getResource("/flows-config.json");
        return Any.pack(ProtobufUtil.fromJson(Resources.toString(url, StandardCharsets.UTF_8), FlowsConfig.class));
    }


    public void execute(final String resource, final Consumer<ByteBuf> consumer) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(consumer);

        final URL resourceURL = getClass().getResource(resource);
        Objects.requireNonNull(resourceURL);

        try {
            try (final FileChannel channel = FileChannel.open(Paths.get(resourceURL.toURI()))) {
                final ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
                channel.read(buffer);
                buffer.flip();
                consumer.accept(Unpooled.wrappedBuffer(buffer));
            }

        } catch (final URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
