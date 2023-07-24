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
 */

package org.opennms.horizon.alertservice.stepdefs;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;

import java.util.concurrent.TimeUnit;

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
