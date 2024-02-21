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
package org.opennms.horizon.inventory.service.discovery.active;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.mapper.discovery.ActiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ActiveDiscoveryService {
    private final ActiveDiscoveryRepository repository;
    private final ActiveDiscoveryMapper mapper;

    @Transactional(readOnly = true)
    public List<ActiveDiscoveryDTO> getActiveDiscoveries(String tenantId) {
        List<ActiveDiscovery> discoveries = repository.findByTenantIdOrderById(tenantId);
        return mapper.modelToDto(discoveries);
    }

    @Transactional
    public void deleteActiveDiscovery(String tenantId, long id) {
        repository.findByTenantIdAndId(tenantId, id).ifPresentOrElse(repository::delete, () -> {
            throw new EntityNotFoundException(String.format("active discovery id %d not found", id));
        });
    }
}
