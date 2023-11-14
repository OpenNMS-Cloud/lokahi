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

package org.opennms.horizon.alertservice.service;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alertservice.db.entity.Alert;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.events.proto.Event;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReductionKeyService {
    public static final String ARCHIVE_SUFFIX = ":archive:";

    public String renderArchiveReductionKey(Alert alert, Event event) {
        String archiveSuffix = ARCHIVE_SUFFIX + event.getProducedTimeMs();
        return alert.getReductionKey() == null ? archiveSuffix : alert.getReductionKey() + archiveSuffix;
    }

    public String renderArchiveClearKey(Alert alert, Event event) {
        return alert.getClearKey() == null ? null : alert.getClearKey() + ARCHIVE_SUFFIX + event.getProducedTimeMs();
    }

    public String renderReductionKey(
        @NonNull Event event, @NonNull AlertDefinition alertDefinition
    ) {
        return String.format(
            alertDefinition.getReductionKey(),
            event.getTenantId(),
            event.getUei(),
            event.getNodeId(),
            alertDefinition.getAlertCondition().getRule().getPolicy().getId()
        );
    }

    public @Nullable String renderClearKey(Event event, AlertDefinition alertDefinition) {
        if (Strings.isNullOrEmpty(alertDefinition.getClearKey())) {
            return null;
        }
        return String.format(
            alertDefinition.getClearKey(),
            event.getTenantId(),
            event.getNodeId(),
            alertDefinition.getAlertCondition().getRule().getPolicy().getId()
        );
    }
}
