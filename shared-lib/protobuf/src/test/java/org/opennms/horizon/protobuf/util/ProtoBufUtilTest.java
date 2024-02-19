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
package org.opennms.horizon.protobuf.util;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.Parameter;
import org.opennms.sink.flows.contract.ParserConfig;

public class ProtoBufUtilTest {
    @Test
    public void testRoundTrip() throws InvalidProtocolBufferException {
        ListenerConfig listenerConfig = ListenerConfig.newBuilder()
                .setName("Netflow-5-UDP-8877")
                .setClassName("org.opennms.netmgt.telemetry.listeners.UdpListener")
                .setEnabled(true)
                .addParameters(Parameter.newBuilder().setKey("port").setValue("8877"))
                .addParsers(ParserConfig.newBuilder()
                        .setName("Netflow-5-Parser")
                        .setClassName("org.opennms.netmgt.telemetry.protocols.netflow.parser.Netflow5UdpParser"))
                .build();
        FlowsConfig flowsConfig =
                FlowsConfig.newBuilder().addListeners(listenerConfig).build();
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
