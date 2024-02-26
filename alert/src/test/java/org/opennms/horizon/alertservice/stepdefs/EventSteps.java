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
package org.opennms.horizon.alertservice.stepdefs;

import io.cucumber.java.en.When;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;

@Slf4j
@RequiredArgsConstructor
public class EventSteps {

    private final BackgroundSteps background;
    private final TenantSteps tenantSteps;
    private final KafkaTestHelper kafkaTestHelper;

    @When("An event is sent with UEI {string} on node {int}")
    public void sendEvent(String uei, int nodeId) {
        sendEvent(uei, nodeId, System.currentTimeMillis());
    }

    @When("An event is sent with UEI {string} on node {int} with produced time {int} {} ago")
    public void sendEvent(String uei, int nodeId, int duration, TimeUnit timeUnit) {
        long producedTimeMs = System.currentTimeMillis() - timeUnit.toMillis(duration);
        sendEvent(uei, nodeId, producedTimeMs);
    }

    public void sendEvent(String uei, int nodeId, long producedTimeMs) {
        EventLog eventLog = EventLog.newBuilder()
                .setTenantId(tenantSteps.getTenantId())
                .addEvents(Event.newBuilder()
                        .setTenantId(tenantSteps.getTenantId())
                        .setProducedTimeMs(producedTimeMs)
                        .setNodeId(nodeId)
                        .setUei(uei))
                .build();

        kafkaTestHelper.sendToTopic(background.getEventTopic(), eventLog.toByteArray(), tenantSteps.getTenantId());
    }
}
