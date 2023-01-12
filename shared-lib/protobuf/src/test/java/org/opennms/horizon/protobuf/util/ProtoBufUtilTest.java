/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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
package org.opennms.horizon.protobuf.util;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.opennms.sink.flows.contract.AdapterConfig;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.PackageConfig;
import org.opennms.sink.flows.contract.Parameter;
import org.opennms.sink.flows.contract.ParserConfig;
import org.opennms.sink.flows.contract.QueueConfig;

public class ProtoBufUtilTest {
    @Test
    public void testRoundTrip() throws InvalidProtocolBufferException {
        ListenerConfig listenerConfig = ListenerConfig.newBuilder()
            .setName("Netflow-5-UDP-8877")
            .setClassName("org.opennms.netmgt.telemetry.listeners.UdpListener")
            .setEnabled(true)
            .addParameters(Parameter.newBuilder().setKey("port").setValue("8877"))
            .addParsers(
                ParserConfig.newBuilder()
                    .setName("Netflow-5-Parser")
                    .setClassName("org.opennms.netmgt.telemetry.protocols.netflow.parser.Netflow5UdpParser")
                    .setQueue(
                        QueueConfig.newBuilder()
                            .setName("Netflow-5")
                            .addAdapters(AdapterConfig.newBuilder()
                                .setName("Netflow-5-Adapter")
                                .setClassName("org.opennms.netmgt.telemetry.protocols.netflow.adapter.netflow5.Netflow5Adapter")
                                .setEnabled(true)
                                .addParameters(Parameter.newBuilder().setKey("applicationDataCollection").setValue("false"))
                                .addParameters(Parameter.newBuilder().setKey("applicationThresholding").setValue("false"))
                                .addPackages(PackageConfig.newBuilder()
                                    .setName("Netflow-5-Default")
                                )
                            )
                    )
            ).build();
        FlowsConfig flowsConfig = FlowsConfig.newBuilder().addListeners(listenerConfig).build();
        String json = ProtobufUtil.toJson(flowsConfig);
        Assert.assertTrue("Json size should not empty.", json.length() > 0);
        FlowsConfig convertedFlowsConfig = ProtobufUtil.fromJson(json, FlowsConfig.class);
        Assert.assertEquals(flowsConfig, convertedFlowsConfig);
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testInvalidInput() throws InvalidProtocolBufferException {
        ProtobufUtil.fromJson("{", FlowsConfig.class);
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testInvalidClass() throws InvalidProtocolBufferException {
        ProtobufUtil.fromJson(ProtobufUtil.toJson(FlowsConfig.newBuilder().build()), GeneratedMessageV3.class);
    }
}
