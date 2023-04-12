/*
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
 *
 */

package org.opennms.horizon.minion.grpc;

import com.codahale.metrics.MetricRegistry;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.opentracing.Tracer;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.minion.grpc.ssl.MinionGrpcSslContextBuilderFactory;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;

import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WARNING: this test does not provide complete coverage of the MinionGrpcClient.
 */
public class MinionGrpcClientTest {

    private MinionGrpcClient target;

    private MetricRegistry mockMetricRegistry;
    private Tracer mockTracer;
    private MinionGrpcSslContextBuilderFactory mockMinionGrpcSslContextBuilderFactory;
    private SslContextBuilder mockSslContextBuilder;

    private IpcIdentity testIpcIdentity;

    @BeforeEach
    public void setUp() {
        mockMetricRegistry = Mockito.mock(MetricRegistry.class);
        mockTracer = Mockito.mock(Tracer.class);
        mockMinionGrpcSslContextBuilderFactory = Mockito.mock(MinionGrpcSslContextBuilderFactory.class);
        mockSslContextBuilder = Mockito.mock(SslContextBuilder.class);

        testIpcIdentity = new MinionIpcIdentity("x-system-id-x", "x-location-x");

        Mockito.when(mockMinionGrpcSslContextBuilderFactory.create()).thenReturn(mockSslContextBuilder);

        target = new MinionGrpcClient(testIpcIdentity, mockMetricRegistry, mockTracer, mockMinionGrpcSslContextBuilderFactory);
    }

    @Test
    void testStartTls() throws Exception {
        //
        // Setup Test Data and Interactions
        //

        try (var logCaptor = LogCaptor.forClass(MinionGrpcClient.class)) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.setTlsEnabled(true);
            target.start();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Minion at location {} with systemId {} started", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 2) &&
                        (logEvent.getArguments().get(0).equals("x-location-x")) &&
                        (logEvent.getArguments().get(1).equals("x-system-id-x"))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }
}
