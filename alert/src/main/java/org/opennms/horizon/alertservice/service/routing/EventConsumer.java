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
package org.opennms.horizon.alertservice.service.routing;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Context;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(EventConsumer.class);
    private final AlertService alertService;

    @KafkaListener(
            topics = {"${kafka.topics.trap-event}", "${kafka.topics.internal-event}"},
            concurrency = "1")
    public void receiveMessage(@Payload byte[] data) {
        try {
            EventLog eventLog = EventLog.parseFrom(data);
            eventLog.getEventsList().forEach(e -> {
                if (Strings.isNullOrEmpty(e.getTenantId())) {
                    LOG.warn("TenantId is empty, dropping event: {}", e);
                    return;
                }

                if (e.getNodeId() <= 0) {
                    LOG.warn("Received an event for unknown device, dropping event: {}", e);
                    return;
                }

                // As this isn't a grpc call, there isn't a grpc context. Create one, and place the tenantId in it.
                Context.current()
                        .withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, e.getTenantId())
                        .run(() -> alertService.reduceEvent(e));
            });
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing EventLog. Payload: {}", Arrays.toString(data), e);
        }
    }
}
