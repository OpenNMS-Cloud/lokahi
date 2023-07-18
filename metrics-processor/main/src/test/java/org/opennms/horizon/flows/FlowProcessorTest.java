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

package org.opennms.horizon.flows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FlowProcessorTest {

    public static final String TENANT_ID = "the_big_brother";

    @Mock
    private Pipeline pipeline;

    @Mock
    private TenantMetricsTracker metricsTracker;
    private FlowProcessor processor;

    @BeforeEach
    public void setup() {
        processor = new FlowProcessor(pipeline, metricsTracker);
    }

    @Test
    void testFlowsSampling() throws Exception {
        TenantLocationSpecificFlowDocumentLog flows =
            TenantLocationSpecificFlowDocumentLog.newBuilder()
                .setTenantId(TENANT_ID)
                .addMessage(
                    FlowDocument.newBuilder()
                        .setSrcAddress("127.0.0.1")
                        .setDstAddress("8.8.8.8")
                ).addMessage(
                    FlowDocument.newBuilder()
                        .setSrcAddress("192.168.0.1")
                        .setDstAddress("1.1.1.1")
                ).build();


        processor.consume(flows.toByteArray());

        verify(pipeline, timeout(5000).only()).process(flows);
        verify(metricsTracker, timeout(5000).times(1)).addTenantFlowReceviedCount(TENANT_ID, 2);
        verify(metricsTracker, timeout(5000).times(1)).addTenantFlowCompletedCount(TENANT_ID, 2);
    }
}
