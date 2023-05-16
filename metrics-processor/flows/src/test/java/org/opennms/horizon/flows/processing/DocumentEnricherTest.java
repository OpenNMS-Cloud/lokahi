/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing;

import com.google.common.collect.Lists;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.spotify.hamcrest.pojo.IsPojo;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.dataplatform.flows.document.Direction;
import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.document.NetflowVersion;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.classification.IpAddr;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DocumentEnricherTest {

    private static FlowDocument createFlowDocument(String sourceIp, String destIp) {
        return createFlowDocument(sourceIp, destIp, 0);
    }

    private static FlowDocument createFlowDocument(String sourceIp, String destIp, final long timeOffset) {
        final var now = Instant.now();
        final var flow = FlowDocument.newBuilder()
            .setReceivedAt(now.plus(timeOffset, ChronoUnit.MILLIS).toEpochMilli())
            .setTimestamp(now.toEpochMilli())
            .setFirstSwitched(UInt64Value.of(now.minus(20_000L, ChronoUnit.MILLIS).toEpochMilli()))
            .setDeltaSwitched(UInt64Value.of(now.minus(10_000L, ChronoUnit.MILLIS).toEpochMilli()))
            .setLastSwitched(UInt64Value.of(now.minus(5_000L, ChronoUnit.MILLIS).toEpochMilli()))
            .setSrcAddress(sourceIp)
            .setSrcPort(UInt32Value.of(510))
            .setDstAddress(destIp)
            .setDstPort(UInt32Value.of(80))
            .setProtocol(UInt32Value.of(6)) // TCP
            .setLocation("Default")
            .setNetflowVersion(NetflowVersion.V5)
            .setTenantId("tenantId")
            .setExporterAddress("127.0.0.1");

        return flow.build();
    }

    @Test
    public void testCreateClassificationRequest() throws InterruptedException {
        final MockDocumentEnricherFactory factory = new MockDocumentEnricherFactory(new HashMap<>());
        final DocumentEnricherImpl enricher = factory.getEnricher();

        final var flowDocument = FlowDocument.newBuilder();

        // verify that null values are handled correctly, see issue HZN-1329
        ClassificationRequest classificationRequest;

        classificationRequest = enricher.createClassificationRequest(flowDocument);
        Assert.assertEquals(false, classificationRequest.isClassifiable());

        flowDocument.setDstPort(UInt32Value.of(123));

        classificationRequest = enricher.createClassificationRequest(flowDocument);
        Assert.assertEquals(false, classificationRequest.isClassifiable());

        flowDocument.setSrcPort(UInt32Value.of(456));

        classificationRequest = enricher.createClassificationRequest(flowDocument);
        Assert.assertEquals(false, classificationRequest.isClassifiable());

        flowDocument.setProtocol(UInt32Value.of(6));

        classificationRequest = enricher.createClassificationRequest(flowDocument);
        Assert.assertEquals(true, classificationRequest.isClassifiable());
    }

    @Test
    public void testDirection() throws InterruptedException {
        final MockDocumentEnricherFactory factory = new MockDocumentEnricherFactory(new HashMap<>());
        final DocumentEnricherImpl enricher = factory.getEnricher();

        final var d1 = FlowDocument.newBuilder()
            .setSrcAddress("1.1.1.1")
            .setSrcPort(UInt32Value.of(1))
            .setDstAddress("2.2.2.2")
            .setDstPort(UInt32Value.of(2))
            .setProtocol(UInt32Value.of(6))
            .setDirection(Direction.INGRESS);

        final ClassificationRequest c1 = enricher.createClassificationRequest(d1);
        Assert.assertEquals(IpAddr.of("1.1.1.1"), c1.getSrcAddress());
        Assert.assertEquals(IpAddr.of("2.2.2.2"), c1.getDstAddress());
        Assert.assertEquals(Integer.valueOf(1), c1.getSrcPort());
        Assert.assertEquals(Integer.valueOf(2), c1.getDstPort());

        final var d2 = FlowDocument.newBuilder()
            .setSrcAddress("1.1.1.1")
            .setSrcPort(UInt32Value.of(1))
            .setDstAddress("2.2.2.2")
            .setDstPort(UInt32Value.of(2))
            .setProtocol(UInt32Value.of(6))
            .setDirection(Direction.EGRESS);

        // check that fields stay as theay are even when EGRESS is used
        final ClassificationRequest c2 = enricher.createClassificationRequest(d2);
        Assert.assertEquals(IpAddr.of("1.1.1.1"), c2.getSrcAddress());
        Assert.assertEquals(IpAddr.of("2.2.2.2"), c2.getDstAddress());
        Assert.assertEquals(Integer.valueOf(1), c2.getSrcPort());
        Assert.assertEquals(Integer.valueOf(2), c2.getDstPort());

        final var d3 = FlowDocument.newBuilder()
            .setSrcAddress("1.1.1.1")
            .setSrcPort(UInt32Value.of(1))
            .setDstAddress("2.2.2.2")
            .setDstPort(UInt32Value.of(2))
            .setProtocol(UInt32Value.of(6));

        final ClassificationRequest c3 = enricher.createClassificationRequest(d3);
        Assert.assertEquals(IpAddr.of("1.1.1.1"), c3.getSrcAddress());
        Assert.assertEquals(IpAddr.of("2.2.2.2"), c3.getDstAddress());
        Assert.assertEquals(Integer.valueOf(1), c3.getSrcPort());
        Assert.assertEquals(Integer.valueOf(2), c3.getDstPort());
    }

    @Test
    public void testClockCorrection() throws InterruptedException {
        final MockDocumentEnricherFactory factory = new MockDocumentEnricherFactory(2400_000L, new HashMap<>());
        final DocumentEnricherImpl enricher = factory.getEnricher();

        final FlowDocument flow1 = createFlowDocument("10.0.0.1", "10.0.0.3");
        final FlowDocument flow2 = createFlowDocument("10.0.0.1", "10.0.0.3", -3600_000L);
        final FlowDocument flow3 = createFlowDocument("10.0.0.1", "10.0.0.3", +3600_000L);

        final List<FlowDocument> flows = Lists.newArrayList(flow1, flow2, flow3);

        final List<FlowDocument> docs = enricher.enrich(flows, "tenantId");
        Assert.assertThat(docs.get(0), Matchers.is(IsPojo.pojo(FlowDocument.class)
                .where(FlowDocument::getTimestamp, Matchers.equalTo(flow1.getTimestamp()))
                .where(FlowDocument::getFirstSwitched, Matchers.equalTo(flow1.getFirstSwitched()))
                .where(FlowDocument::getDeltaSwitched, Matchers.equalTo(flow1.getDeltaSwitched()))
                .where(FlowDocument::getLastSwitched, Matchers.equalTo(flow1.getLastSwitched()))));

        System.out.println(docs.get(1).getTimestamp() + "|" + (flow2.getTimestamp() - 3600_000L));
        Assert.assertThat(docs.get(1), Matchers.is(
            IsPojo.pojo(FlowDocument.class)
                .where(FlowDocument::getTimestamp, Matchers.is(flow2.getTimestamp() - 3600_000L))
                .where(FlowDocument::getFirstSwitched, Matchers.is(UInt64Value.of(flow2.getFirstSwitched().getValue() - 3600_000L)))
                .where(FlowDocument::getDeltaSwitched, Matchers.is(UInt64Value.of(flow2.getDeltaSwitched().getValue() - 3600_000L)))
                .where(FlowDocument::getLastSwitched, Matchers.is(UInt64Value.of(flow2.getLastSwitched().getValue() - 3600_000L)))));

        Assert.assertThat(docs.get(2), Matchers.is(
            IsPojo.pojo(FlowDocument.class)
                .where(FlowDocument::getTimestamp, Matchers.is(flow3.getTimestamp() + 3600_000L))
                .where(FlowDocument::getFirstSwitched, Matchers.is(UInt64Value.of(flow3.getFirstSwitched().getValue() + 3600_000L)))
                .where(FlowDocument::getDeltaSwitched, Matchers.is(UInt64Value.of(flow3.getDeltaSwitched().getValue() + 3600_000L)))
                .where(FlowDocument::getLastSwitched, Matchers.is(UInt64Value.of(flow3.getLastSwitched().getValue() + 3600_000L)))));

        docs.forEach(doc -> {
            Assert.assertEquals(NetflowVersion.V5, doc.getNetflowVersion());
            Assert.assertEquals("tenantId", doc.getTenantId());
        });
    }
}
