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

package org.opennms.horizon.notifications.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
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

import java.time.Instant;
import java.util.HashMap;

@RequiredArgsConstructor
@Service
public class PagerDutyEventFactory {

    private final PagerDutyDao pagerDutyDao;

    @Value("${horizon.pagerduty.client}")
    String client;

    @Value("${horizon.pagerduty.clientURL}")
    String clientURL;

    public PagerDutyEventDTO createEvent(Alert alert) throws NotificationConfigUninitializedException, JsonProcessingException, InvalidProtocolBufferException {
        Instant now = Instant.now();

        PagerDutyEventDTO event = new PagerDutyEventDTO();
        PagerDutyPayloadDTO payload = new PagerDutyPayloadDTO();

        payload.setSummary(getEventSummary(alert));
        payload.setTimestamp(now.toString());
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
        event.setClientUrl(clientURL);

        payload.setCustomDetails(new HashMap<>());
        // Put the whole alert in the payload
        payload.getCustomDetails().put("alert", JsonFormat.printer().includingDefaultValueFields().print(alert));

        event.setPayload(payload);
        return event;
    }

    private String getPagerDutyIntegrationKey(String tenantId) throws NotificationConfigUninitializedException {
        PagerDutyConfigDTO config = pagerDutyDao.getConfig(tenantId);
        return config.getIntegrationKey();
    }

    /**
     * Derives an event summary from an alert. Defaults to the alert's log
     * message, but if blank, returns a summary based on UEI.
     *
     * @return A non-null, non-blank string, because PagerDuty will reject the
     * alert otherwise.
     */
    private String getEventSummary(Alert alert) {
        String logMessage = alert.getLogMessage().trim();
        return Strings.isBlank(logMessage)
            ? String.format("Event: %s", alert.getUei())
            : logMessage;
    }
}
