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
package org.opennms.horizon.datachoices.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.datachoices.dto.ToggleDataChoicesDTO;
import org.opennms.horizon.datachoices.model.DataChoices;
import org.opennms.horizon.datachoices.repository.DataChoicesRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class DataChoicesService {
    private final DataChoicesRepository repository;

    public void toggle(ToggleDataChoicesDTO request, String tenantId) {
        if (request.getToggle()) {
            toggleOn(tenantId);
        } else {
            toggleOff(tenantId);
        }
    }

    private void toggleOn(String tenantId) {
        Optional<DataChoices> dataChoicesOpt = repository.findByTenantId(tenantId);
        if (dataChoicesOpt.isEmpty()) {
            DataChoices dataChoices = new DataChoices();
            dataChoices.setTenantId(tenantId);
            repository.saveAndFlush(dataChoices);
        } else {
            log.warn("DataChoices already toggled on for tenant = {}", tenantId);
        }
    }

    private void toggleOff(String tenantId) {
        Optional<DataChoices> dataChoicesOpt = repository.findByTenantId(tenantId);
        if (dataChoicesOpt.isPresent()) {
            DataChoices dataChoices = dataChoicesOpt.get();
            repository.delete(dataChoices);
        } else {
            log.warn("DataChoices already toggled off for tenant = {}", tenantId);
        }
    }

    @WithSpan
    public void execute() {
        List<String> tenantIds = repository.findAll().stream()
                .map(DataChoices::getTenantId)
                .distinct()
                .collect(Collectors.toList());

        log.info("tenantIds.size() = " + tenantIds.size());
        Span.current().setAttribute("tenant_ids_count", tenantIds.size());

        //        todo: perform queries and send HTTP request to usage-stats-handler
    }
}
