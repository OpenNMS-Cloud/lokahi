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

    public String renderReductionKey(@NonNull Event event, @NonNull AlertDefinition alertDefinition) {
        return String.format(
                alertDefinition.getReductionKey(),
                event.getTenantId(),
                event.getUei(),
                event.getNodeId(),
                alertDefinition.getAlertCondition().getRule().getPolicy().getId());
    }

    public @Nullable String renderClearKey(Event event, AlertDefinition alertDefinition) {
        if (Strings.isNullOrEmpty(alertDefinition.getClearKey())) {
            return null;
        }
        return String.format(
                alertDefinition.getClearKey(),
                event.getTenantId(),
                event.getNodeId(),
                alertDefinition.getAlertCondition().getRule().getPolicy().getId());
    }
}
