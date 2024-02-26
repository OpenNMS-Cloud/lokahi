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
package org.opennms.horizon.inventory.service;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.AzureInterfaceDTO;
import org.opennms.horizon.inventory.mapper.AzureInterfaceMapper;
import org.opennms.horizon.inventory.model.AzureInterface;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.AzureInterfaceRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AzureInterfaceService {
    private final AzureInterfaceRepository modelRepo;
    private final AzureInterfaceMapper mapper;

    public Optional<AzureInterfaceDTO> findByIdAndTenantId(long id, String tenantId) {
        return modelRepo.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public AzureInterface createOrUpdateFromScanResult(
            String tenantId, Node node, AzureScanNetworkInterfaceItem azureScanNetworkInterfaceItem) {
        Objects.requireNonNull(azureScanNetworkInterfaceItem);
        String publicIpId = azureScanNetworkInterfaceItem.hasPublicIpAddress()
                ? azureScanNetworkInterfaceItem.getPublicIpAddress().getName()
                : null;
        return modelRepo
                .findByTenantIdAndPublicIpId(tenantId, publicIpId)
                .map(azure -> {
                    mapper.updateFromScanResult(azure, azureScanNetworkInterfaceItem);
                    return modelRepo.save(azure);
                })
                .orElseGet(() -> {
                    var azure = mapper.scanResultToModel(azureScanNetworkInterfaceItem);
                    azure.setTenantId(tenantId);
                    azure.setNode(node);
                    return modelRepo.save(azure);
                });
    }
}
