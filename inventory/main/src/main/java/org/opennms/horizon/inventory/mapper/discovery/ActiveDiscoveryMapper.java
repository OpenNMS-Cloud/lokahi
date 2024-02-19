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
package org.opennms.horizon.inventory.mapper.discovery;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveDiscoveryMapper {
    private final IcmpActiveDiscoveryMapper icmpActiveDiscoveryMapper;
    private final AzureActiveDiscoveryMapper azureActiveDiscoveryMapper;

    public List<ActiveDiscoveryDTO> modelToDto(List<ActiveDiscovery> list) {
        return list.stream().map(this::modelToDto).toList();
    }

    public ActiveDiscoveryDTO modelToDto(ActiveDiscovery discovery) {
        ActiveDiscoveryDTO.Builder builder = ActiveDiscoveryDTO.newBuilder();
        if (discovery instanceof IcmpActiveDiscovery icmp) {
            builder.setIcmp(icmpActiveDiscoveryMapper.modelToDto(icmp));
        } else if (discovery instanceof AzureActiveDiscovery azure) {
            builder.setAzure(azureActiveDiscoveryMapper.modelToDto(azure));
        }
        return builder.build();
    }
}
