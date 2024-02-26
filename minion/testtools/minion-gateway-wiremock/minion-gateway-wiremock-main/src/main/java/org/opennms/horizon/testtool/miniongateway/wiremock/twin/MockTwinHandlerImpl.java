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
package org.opennms.horizon.testtool.miniongateway.wiremock.twin;

import com.google.protobuf.ByteString;
import java.util.concurrent.atomic.AtomicLong;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockGrpcServiceApi;
import org.opennms.horizon.testtool.miniongateway.wiremock.api.MockTwinHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MockTwinHandlerImpl implements MockTwinHandler {

    @Autowired
    private MockGrpcServiceApi grpcOperations;

    private AtomicLong sessionSeq = new AtomicLong(0);

    @Override
    public void publish(String topic, byte[] content) {
        TwinResponseProto twinResponseProto = TwinResponseProto.newBuilder()
                .setConsumerKey(topic)
                .setTwinObject(ByteString.copyFrom(content))
                .setSessionId("mock-twin-handler-session-" + sessionSeq.getAndIncrement())
                .build();

        CloudToMinionMessage cloudToMinionMessage = CloudToMinionMessage.newBuilder()
                .setTwinResponse(twinResponseProto)
                .build();

        grpcOperations.sendMessageToLocation(cloudToMinionMessage);
    }
}
