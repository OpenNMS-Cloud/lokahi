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
package org.opennms.horizon.notifications.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.notifications.api.dto.PagerDutyEventAction;
import org.opennms.horizon.notifications.api.dto.PagerDutyEventDTO;
import org.opennms.horizon.notifications.api.dto.PagerDutyPayloadDTO;
import org.opennms.horizon.notifications.api.dto.PagerDutySeverity;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationConfigUninitializedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PagerDutyEventFactory {

    private final PagerDutyDao pagerDutyDao;

    private final LokahiUrlUtil lokahiUrlUtil;

    @Value("${horizon.pagerduty.client}")
    String client;

    public PagerDutyEventDTO createEvent(Alert alert)
            throws NotificationConfigUninitializedException, JsonProcessingException, InvalidProtocolBufferException {
        PagerDutyEventDTO event = new PagerDutyEventDTO();
        PagerDutyPayloadDTO payload = new PagerDutyPayloadDTO();

        payload.setSummary(getEventSummary(alert));
        payload.setTimestamp(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli((alert.getLastUpdateTimeMs()))));
        payload.setSeverity(PagerDutySeverity.fromAlertSeverity(alert.getSeverity()));

        // Source: unique location of affected system
        payload.setSource(JsonFormat.printer()
                .omittingInsignificantWhitespace()
                .sortingMapKeys()
                .print(alert.getManagedObject()));
        // Component: component responsible for the event
        payload.setComponent(alert.getManagedObject().getType().name());
        // Group: logical grouping
        payload.setGroup(alert.getLocation());
        // Class: type of event
        payload.setClazz(alert.getUei());

        event.setRoutingKey(getPagerDutyIntegrationKey(alert.getTenantId()));
        event.setDedupKey(alert.getReductionKey());

        if (Severity.CLEARED.equals(alert.getSeverity()) || AlertType.CLEAR.equals(alert.getType())) {
            event.setEventAction(PagerDutyEventAction.RESOLVE);
        } else if (alert.getIsAcknowledged()) {
            event.setEventAction(PagerDutyEventAction.ACKNOWLEDGE);
        } else {
            event.setEventAction(PagerDutyEventAction.TRIGGER);
        }

        // TODO: We need to determine what the external facing URL is for the client
        event.setClient(client);
        event.setClientUrl(lokahiUrlUtil.getAlertstUrl(alert));

        payload.setCustomDetails(new HashMap<>());
        // Put the whole alert in the payload
        payload.getCustomDetails()
                .put("alert", JsonFormat.printer().includingDefaultValueFields().print(alert));

        event.setPayload(payload);
        return event;
    }

    private String getPagerDutyIntegrationKey(String tenantId) throws NotificationConfigUninitializedException {
        PagerDutyConfigDTO config = pagerDutyDao.getConfig(tenantId);
        return config.getIntegrationKey();
    }

    /**
     * Derives an event summary from an alert. Defaults to the alert's log
     * message, but if blank, returns a summary align with email template (skipped pagerduty mandatory fields)
     *
     * @return A non-null, non-blank string, because PagerDuty will reject the
     * alert otherwise.
     */
    private String getEventSummary(Alert alert) {
        String logMessage = alert.getLogMessage().trim();
        return Strings.isBlank(logMessage)
                ? String.format(
                        "Node Name: %s, Description: %s, Started: %s, Policy Name: %s, Rule Name: %s",
                        alert.getNodeName(),
                        alert.getDescription(),
                        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli((alert.getFirstEventTimeMs()))),
                        alert.getPolicyNameList(),
                        alert.getRuleNameList())
                : logMessage;
    }
}
